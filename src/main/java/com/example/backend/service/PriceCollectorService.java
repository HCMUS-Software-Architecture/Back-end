package com.example.backend.service;

import com.example.backend.dto.CandleDto;
import com.example.backend.dto.binance.BinanceKlineEvent;
import com.example.backend.entity.PriceCandle;
import com.example.backend.entity.PriceTick;
import com.example.backend.repository.PriceCandleRepository;
import com.example.backend.repository.PriceTickRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCollectorService {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PriceCandleRepository priceCandleRepository;
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    private final String[] supportedInterval = {"1m", "3m", "5m", "15m", "30m", "1h"};

    // Dùng Combined Stream URL
    @Value("${binance.ws.url:wss://stream.binance.com:9443/stream?streams=}")
    private String binanceBaseUrl;

    @Value("${price.symbols:btcusdt,ethusdt}")
    private String symbolsConfig;

    private WebSocketClient webSocketClient;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        List<String> symbols = List.of(symbolsConfig.split(","));
        symbols.forEach(this::connectToBinanceStream);
    }

    @PreDestroy
    public void cleanup() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        executorService.shutdown();
    }

    public void connectToBinanceStream(String symbol) {

        List<String> streamParams = new ArrayList<>();

        for(String interval : supportedInterval) {
            streamParams.add(symbol.toLowerCase() + "@kline_" + interval);
        }

        String fullUrl = binanceBaseUrl + String.join("/", streamParams);
        log.info("connect binance websocket for full url: {}", fullUrl);

        try {
            webSocketClient = new WebSocketClient(new URI(fullUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("Connected to Binance Combined Stream: {}", fullUrl);
                }

                @Override
                public void onMessage(String message) {
                    // Binance trả về dạng: {"stream":"btcusdt@kline_1m", "data": {...}}
                    handleKlineMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("Disconnected from Binance: {}", reason);
                    scheduleReconnect(symbol);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket error: {}", ex.getMessage());
                }
            };

            webSocketClient.connect();

        } catch (Exception e) {
            log.error("Failed to init WebSocket: {}", e.getMessage());
        }
    }

    private void handleKlineMessage(String message) {
        try {
            // Combined stream trả về JSON có field "data". Ta chỉ cần parse phần "data"
            // Hoặc parse cả cục nếu dùng DTO wrapper.
            // Cách nhanh nhất là lấy node "data":
            String dataStr = objectMapper.readTree(message).get("data").toString();
            BinanceKlineEvent event = objectMapper.readValue(dataStr, BinanceKlineEvent.class);

            if ("kline".equals(event.getEventType())) {
                processKline(event);
            }

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    private void processKline(BinanceKlineEvent event) {
        BinanceKlineEvent.BinanceKlineData kline = event.getKline();
        String symbol = event.getSymbol();
        String interval = kline.getInterval();

        // 1. Map sang CandleDto để bắn socket cho FE (Real-time update)
        CandleDto candleDto = new CandleDto();
        candleDto.setSymbol(symbol);
        candleDto.setOpen(new BigDecimal(kline.getOpen()));
        candleDto.setHigh(new BigDecimal(kline.getHigh()));
        candleDto.setLow(new BigDecimal(kline.getLow()));
        candleDto.setClose(new BigDecimal(kline.getClose())); // Giá realtime là Close
        candleDto.setVolume(new BigDecimal(kline.getVolume()));
        candleDto.setOpenTime(kline.getOpenTime());

        // Topic: /topic/candles/1m/btcusdt
        String destination = "/topic/candles/" + interval + "/" + symbol.toLowerCase();
        simpMessagingTemplate.convertAndSend(destination, candleDto);

        // 2. Nếu nến đã đóng (isClosed = true) -> Lưu vào DB
        if (kline.isClosed()) {
            saveClosedCandle(kline, symbol);
        }
    }

    private void saveClosedCandle(BinanceKlineEvent.BinanceKlineData kline, String symbol) {
        try {
            PriceCandle entity = PriceCandle.builder()
                    .symbol(symbol.toUpperCase())
                    .interval(kline.getInterval())
                    .openTime(Instant.ofEpochMilli(kline.getOpenTime()))
                    .closeTime(Instant.ofEpochMilli(kline.getCloseTime()))
                    .open(new BigDecimal(kline.getOpen()))
                    .high(new BigDecimal(kline.getHigh()))
                    .low(new BigDecimal(kline.getLow()))
                    .close(new BigDecimal(kline.getClose()))
                    .volume(new BigDecimal(kline.getVolume()))
                    .trades(kline.getTrades())
                    .createdAt(LocalDateTime.now())
                    .build();

            priceCandleRepository.save(entity);
            log.info("Saved closed candle: {} {}", symbol, kline.getInterval());
        } catch (Exception e) {
            log.error("Failed to save candle: {}", e.getMessage());
        }
    }

    private void scheduleReconnect(String symbol) {
        executorService.schedule(() -> {
            connectToBinanceStream(symbol);
        }, 5, TimeUnit.SECONDS);
    }


    public List<String> getAllSymbols() {
        return List.of(symbolsConfig.split(","));
    }
    public List<String> getAllSupportedIntervals() {
        return List.of(supportedInterval);
    }
}
