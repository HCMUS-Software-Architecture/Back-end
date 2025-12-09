package com.example.backend.service;

import com.example.backend.entity.PriceTick;
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
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCollectorService {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final TickBufferService tickBufferService;
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    @Value("${binance.ws.url:wss://stream.binance.com:9443/ws}")
    private String binanceWsUrl;

    @Value("${price.symbols:btcusdt,ethusdt}")
    private String symbolsConfig;

    private final ConcurrentHashMap<String, WebSocketClient> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BigDecimal> latestPrices = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        List<String> symbols = List.of(symbolsConfig.split(","));
        symbols.forEach(this::connectToSymbol);
    }

    @PreDestroy
    public void cleanup() {
        connections.values().forEach(WebSocketClient::close);
        executorService.shutdown();
    }

    public void connectToSymbol(String symbol) {
        String wsUrl = binanceWsUrl + "/" + symbol.toLowerCase() + "@trade";

        try {
            WebSocketClient client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("Connected to Binance WebSocket for {}", symbol);
                }

                @Override
                public void onMessage(String message) {
                    handleTradeMessage(symbol, message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("Disconnected from Binance WebSocket for {}: {}", symbol, reason);
                    // Reconnect after delay
                    scheduleReconnect(symbol);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket error for {}: {}", symbol, ex.getMessage());
                }
            };

            client.connect();
            connections.put(symbol, client);

        } catch (Exception e) {
            log.error("Failed to connect to Binance for {}: {}", symbol, e.getMessage());
        }
    }


    protected void handleTradeMessage(String symbol, String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            BigDecimal price = new BigDecimal(node.get("p").asText());
            BigDecimal quantity = new BigDecimal(node.get("q").asText());
            long timestamp = node.get("T").asLong();

            // Store tick
            PriceTick tick = PriceTick.builder()
                    .symbol(symbol.toUpperCase())
                    .price(price)
                    .quantity(quantity)
                    .timestamp(Instant.ofEpochMilli(timestamp))
                    .exchange("binance")
                    .build();

            tickBufferService.addTick(tick);

            // Update latest price
            latestPrices.put(symbol.toUpperCase(), price);

            // Broadcast to WebSocket clients
            simpMessagingTemplate.convertAndSend(
                    "/topic/prices/" + symbol.toLowerCase(),
                    tick
            );

        } catch (Exception e) {
            log.error("Failed to process trade message: {}", e.getMessage());
        }
    }

    private void scheduleReconnect(String symbol) {

        // Simple reconnect logic - in production use exponential backoff
        executorService.schedule(() -> {
            connectToSymbol(symbol);
        }, 5, TimeUnit.SECONDS);
    }

    public BigDecimal getLatestPrice(String symbol) {
        return latestPrices.get(symbol.toUpperCase());
    }
}
