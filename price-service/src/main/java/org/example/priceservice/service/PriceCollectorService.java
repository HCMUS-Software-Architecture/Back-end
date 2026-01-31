package org.example.priceservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.priceservice.client.BinanceApiClient;
import org.example.priceservice.dto.BinanceKlineEvent;
import org.example.priceservice.dto.CandleDto;
import org.example.priceservice.entity.PriceCandle;
import org.example.priceservice.repository.PriceCandleRepository;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("collector")
public class PriceCollectorService {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PriceCandleRepository priceCandleRepository;
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    private final String[] supportedInterval = { "1m", "3m", "5m", "15m", "30m", "1h" };
    private final Map<String, WebSocketClient> webSocketClientMap = new ConcurrentHashMap<>();

    // Dùng Combined Stream URL
    @Value("${binance.ws.url:wss://stream.binance.com:9443/stream?streams=}")
    private String binanceBaseUrl;

    @Value("${price.symbols:btcusdt,ethusdt}")
    private String symbolsConfig;

    @EventListener(ApplicationReadyEvent.class)
    private void init() {
        List<String> symbols = List.of(symbolsConfig.split(","));
        symbols.forEach(this::connectToBinanceStream);
    }

    @PreDestroy
    private void cleanup() {
        webSocketClientMap.forEach((symbol, client) -> {
            if (client != null && client.isOpen()) {
                client.close();
            }
        });
        executorService.shutdown();
    }

    private void connectToBinanceStream(String symbol) {
        log.info("Collector service is activated due to profile's name of the service is collector-service");

        if (webSocketClientMap.containsKey(symbol)) {
            log.info("An existing WebSocket Client has been opened");
            return;
        }

        List<String> streamParams = new ArrayList<>();

        for (String interval : supportedInterval) {
            streamParams.add(symbol.toLowerCase() + "@kline_" + interval);
        }

        String fullUrl = binanceBaseUrl + String.join("/", streamParams);
        log.info("connect binance websocket for full url: {}", fullUrl);

        try {
            WebSocketClient webSocketClient = new WebSocketClient(new URI(fullUrl)) {
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
                    webSocketClientMap.remove(symbol);
                    scheduleReconnect(symbol);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket error: {}", ex.getMessage());
                }
            };

            webSocketClient.connect();
            webSocketClientMap.put(symbol, webSocketClient);

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
        String destination = "/topic/candles." + interval + "." + symbol.toLowerCase();
        simpMessagingTemplate.convertAndSend(destination, candleDto);

        // 2. Nếu nến đã đóng (isClosed = true) -> Lưu vào DB
        if (kline.isClosed()) {
            saveClosedCandle(kline, symbol);
        }
    }

    private void saveClosedCandle(BinanceKlineEvent.BinanceKlineData kline, String symbol) {
        // try {
        //     PriceCandle entity = PriceCandle.builder()
        //             .symbol(symbol.toUpperCase())
        //             .interval(kline.getInterval())
        //             .openTime(Instant.ofEpochMilli(kline.getOpenTime()))
        //             .closeTime(Instant.ofEpochMilli(kline.getCloseTime()))
        //             .open(new BigDecimal(kline.getOpen()))
        //             .high(new BigDecimal(kline.getHigh()))
        //             .low(new BigDecimal(kline.getLow()))
        //             .close(new BigDecimal(kline.getClose()))
        //             .volume(new BigDecimal(kline.getVolume()))
        //             .trades(kline.getTrades())
        //             .createdAt(LocalDateTime.now())
        //             .build();

        //     priceCandleRepository.save(entity);
        //     log.info("Saved closed candle: {} {}", symbol, kline.getInterval());
        // } catch (Exception e) {
        //     log.error("Failed to save candle: {}", e.getMessage());
        // }
    }

    private void scheduleReconnect(String symbol) {
        executorService.schedule(() -> {
            connectToBinanceStream(symbol);
        }, 5, TimeUnit.SECONDS);
    }
}
