# Phase 2: Database Optimization - Implementation Guide

**Target Scale**: 10-1,000 users  
**Architecture**: Monolith with specialized data stores  
**Duration**: Weeks 3-4 (December 14 - December 27, 2025)

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [MongoDB Integration](#mongodb-integration)
4. [Redis Caching](#redis-caching)
5. [Price Collector Implementation](#price-collector-implementation)
6. [WebSocket for Real-time Updates](#websocket-for-real-time-updates)
7. [Candle Aggregation](#candle-aggregation)
8. [Deployment](#deployment)
9. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
10. [References](#references)

---

## Overview

### Goals

- Add MongoDB for raw article storage and flexible document schemas
- Integrate Redis for caching and session management
- Implement Price Collector with Binance API integration
- Add WebSocket support for real-time price updates
- Implement candle aggregation (1m, 5m, 15m, 1h, 1d)

### Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│                 Spring Boot App                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ REST API │ │ Crawler  │ │ Price Collector  │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
│  ┌──────────┐ ┌──────────┐                      │
│  │ WebSocket│ │   NLP    │                      │
│  └──────────┘ └──────────┘                      │
└─────────────────────────────────────────────────┘
         │              │              │
    ┌────┴────┐    ┌────┴────┐    ┌────┴────┐
    │PostgreSQL│    │MongoDB  │    │ Redis   │
    │(prices)  │    │(articles)│   │(cache)  │
    └──────────┘    └──────────┘   └─────────┘
```

### Epics for Phase 2

| Epic | Description | Priority |
|------|-------------|----------|
| E2.1 | MongoDB Integration for Articles | High |
| E2.2 | Redis Caching Layer | High |
| E2.3 | Price Collector (Binance API) | High |
| E2.4 | WebSocket Real-time Updates | High |
| E2.5 | Candle Aggregation | Medium |
| E2.6 | Cache-aside Pattern Implementation | Medium |

---

## Prerequisites

### Additional Software for Phase 2

| Software | Version | Purpose |
|----------|---------|---------|
| MongoDB | 6.0+ | Document storage |
| Redis | 7.0+ | Caching |
| Docker Compose | 2.0+ | Container orchestration |

### Updated Docker Compose

Create `docker/docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: trading-postgres
    environment:
      POSTGRES_DB: trading
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  mongodb:
    image: mongo:6.0
    container_name: trading-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin
      MONGO_INITDB_DATABASE: trading
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: trading-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  mongo_data:
  redis_data:
```

### Start Infrastructure

**Bash (Linux/macOS):**
```bash
cd docker
docker compose up -d

# Verify all services are running
docker compose ps
docker compose logs -f
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location docker
docker compose up -d

# Verify all services are running
docker compose ps
docker compose logs -f
```

---

## MongoDB Integration

### Step 1: Add Dependencies

Update `pom.xml`:

```xml
<!-- MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### Step 2: Configure MongoDB

Update `src/main/resources/application-dev.properties`:

```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://admin:admin@localhost:27017/trading?authSource=admin
spring.data.mongodb.database=trading

# MongoDB indexes
spring.data.mongodb.auto-index-creation=true
```

### Step 3: Create MongoDB Document

Create `src/main/java/com/example/backend/model/ArticleDocument.java`:

```java
package com.example.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String url;
    
    private String title;
    private String body;
    
    @Indexed
    private String source;
    
    private String rawHtml;
    
    @Indexed
    private LocalDateTime publishedAt;
    
    private LocalDateTime crawledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Flexible metadata
    private Map<String, Object> metadata;
    
    // NLP results (Phase 3+)
    private SentimentResult sentiment;
    private List<String> entities;
    private List<String> symbols;
    private List<String> tags;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SentimentResult {
        private String label; // bullish, bearish, neutral
        private double score;
        private String model;
    }
}
```

### Step 4: Create MongoDB Repository

Create `src/main/java/com/example/backend/repository/ArticleDocumentRepository.java`:

```java
package com.example.backend.repository;

import com.example.backend.model.ArticleDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleDocumentRepository extends MongoRepository<ArticleDocument, String> {
    
    Optional<ArticleDocument> findByUrl(String url);
    
    boolean existsByUrl(String url);
    
    Page<ArticleDocument> findBySource(String source, Pageable pageable);
    
    Page<ArticleDocument> findByPublishedAtBetween(
        LocalDateTime start, 
        LocalDateTime end, 
        Pageable pageable
    );
    
    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'body': { $regex: ?0, $options: 'i' } } ] }")
    Page<ArticleDocument> searchByKeyword(String keyword, Pageable pageable);
    
    List<ArticleDocument> findBySymbolsContaining(String symbol);
    
    @Query("{ 'sentiment.label': ?0 }")
    Page<ArticleDocument> findBySentiment(String sentiment, Pageable pageable);
}
```

---

## Redis Caching

### Step 1: Add Dependencies

Update `pom.xml`:

```xml
<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### Step 2: Configure Redis

Update `src/main/resources/application-dev.properties`:

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=300000
spring.cache.redis.cache-null-values=false
```

### Step 3: Create Cache Configuration

Create `src/main/java/com/example/backend/config/CacheConfig.java`:

```java
package com.example.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                    )
                )
                .disableCachingNullValues();
        
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Articles cache - 5 minutes
        cacheConfigurations.put("articles", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Price cache - 10 seconds (real-time data)
        cacheConfigurations.put("prices", defaultConfig.entryTtl(Duration.ofSeconds(10)));
        
        // Candles cache - 1 minute
        cacheConfigurations.put("candles", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
```

### Step 4: Apply Caching to Services

Update `src/main/java/com/example/backend/service/ArticleService.java`:

```java
package com.example.backend.service;

import com.example.backend.model.ArticleDocument;
import com.example.backend.repository.ArticleDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    
    private final ArticleDocumentRepository articleRepository;
    
    @Cacheable(value = "articles", key = "'all-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ArticleDocument> getAllArticles(Pageable pageable) {
        log.debug("Cache miss - fetching articles from database");
        return articleRepository.findAll(pageable);
    }
    
    @Cacheable(value = "articles", key = "'id-' + #id")
    public Optional<ArticleDocument> getArticleById(String id) {
        return articleRepository.findById(id);
    }
    
    @CacheEvict(value = "articles", allEntries = true)
    public ArticleDocument saveArticle(ArticleDocument article) {
        if (articleRepository.existsByUrl(article.getUrl())) {
            log.info("Article already exists: {}", article.getUrl());
            return articleRepository.findByUrl(article.getUrl()).orElse(null);
        }
        return articleRepository.save(article);
    }
    
    @Cacheable(value = "articles", key = "'source-' + #source + '-' + #pageable.pageNumber")
    public Page<ArticleDocument> getArticlesBySource(String source, Pageable pageable) {
        return articleRepository.findBySource(source, pageable);
    }
    
    public Page<ArticleDocument> searchArticles(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable);
    }
}
```

---

## Price Collector Implementation

### Step 1: Add Dependencies

Update `pom.xml`:

```xml
<!-- WebSocket Client for Binance -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.6</version>
</dependency>
```

### Step 2: Create Price Models

Create `src/main/java/com/example/backend/model/PriceTick.java`:

```java
package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_ticks", indexes = {
    @Index(name = "idx_price_ticks_symbol_time", columnList = "symbol, timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceTick {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 20)
    private String symbol;
    
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal quantity;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    private String exchange;
}
```

Create `src/main/java/com/example/backend/model/PriceCandle.java`:

```java
package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_candles", indexes = {
    @Index(name = "idx_candles_symbol_interval_time", columnList = "symbol, interval, openTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCandle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 20)
    private String symbol;
    
    @Column(nullable = false, length = 10)
    private String interval; // 1m, 5m, 15m, 1h, 4h, 1d
    
    @Column(nullable = false)
    private Instant openTime;
    
    private Instant closeTime;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal open;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal high;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal low;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal close;
    
    @Column(precision = 30, scale = 8)
    private BigDecimal volume;
    
    private Integer trades;
}
```

### Step 3: Create Price Collector Service

Create `src/main/java/com/example/backend/service/PriceCollectorService.java`:

```java
package com.example.backend.service;

import com.example.backend.model.PriceTick;
import com.example.backend.repository.PriceTickRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCollectorService {
    
    private final PriceTickRepository priceTickRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${binance.ws.url:wss://stream.binance.com:9443/ws}")
    private String binanceWsUrl;
    
    @Value("${price.symbols:btcusdt,ethusdt}")
    private String symbolsConfig;
    
    private final ConcurrentHashMap<String, WebSocketClient> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BigDecimal> latestPrices = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        List<String> symbols = List.of(symbolsConfig.split(","));
        symbols.forEach(this::connectToSymbol);
    }
    
    @PreDestroy
    public void cleanup() {
        connections.values().forEach(WebSocketClient::close);
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
    
    private void handleTradeMessage(String symbol, String message) {
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
            
            priceTickRepository.save(tick);
            
            // Update latest price
            latestPrices.put(symbol.toUpperCase(), price);
            
            // Broadcast to WebSocket clients
            messagingTemplate.convertAndSend(
                "/topic/prices/" + symbol.toLowerCase(),
                tick
            );
            
        } catch (Exception e) {
            log.error("Failed to process trade message: {}", e.getMessage());
        }
    }
    
    private void scheduleReconnect(String symbol) {
        // Simple reconnect logic - in production use exponential backoff
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                connectToSymbol(symbol);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public BigDecimal getLatestPrice(String symbol) {
        return latestPrices.get(symbol.toUpperCase());
    }
}
```

---

## WebSocket for Real-time Updates

### Step 1: Configure WebSocket

Create `src/main/java/com/example/backend/config/WebSocketConfig.java`:

```java
package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/prices")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

### Step 2: Create Price Controller

Create `src/main/java/com/example/backend/controller/PriceController.java`:

```java
package com.example.backend.controller;

import com.example.backend.model.PriceCandle;
import com.example.backend.service.PriceCandleService;
import com.example.backend.service.PriceCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {
    
    private final PriceCollectorService priceCollectorService;
    private final PriceCandleService candleService;
    
    @GetMapping("/current/{symbol}")
    public ResponseEntity<Map<String, Object>> getCurrentPrice(@PathVariable String symbol) {
        BigDecimal price = priceCollectorService.getLatestPrice(symbol.toUpperCase());
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "symbol", symbol.toUpperCase(),
            "price", price,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    @GetMapping("/historical")
    public ResponseEntity<List<PriceCandle>> getHistoricalCandles(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1h") String interval,
            @RequestParam(defaultValue = "100") int limit
    ) {
        List<PriceCandle> candles = candleService.getCandles(symbol, interval, limit);
        return ResponseEntity.ok(candles);
    }
    
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAvailableSymbols() {
        return ResponseEntity.ok(List.of("BTCUSDT", "ETHUSDT", "BNBUSDT"));
    }
}
```

---

## Candle Aggregation

### Step 1: Create Candle Service

Create `src/main/java/com/example/backend/service/PriceCandleService.java`:

```java
package com.example.backend.service;

import com.example.backend.model.PriceCandle;
import com.example.backend.model.PriceTick;
import com.example.backend.repository.PriceCandleRepository;
import com.example.backend.repository.PriceTickRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCandleService {
    
    private final PriceTickRepository tickRepository;
    private final PriceCandleRepository candleRepository;
    
    // Aggregate every minute for 1m candles
    @Scheduled(cron = "0 * * * * *")
    public void aggregate1mCandles() {
        aggregateCandles("1m", 1);
    }
    
    // Aggregate every 5 minutes for 5m candles
    @Scheduled(cron = "0 */5 * * * *")
    public void aggregate5mCandles() {
        aggregateCandles("5m", 5);
    }
    
    // Aggregate every hour for 1h candles
    @Scheduled(cron = "0 0 * * * *")
    public void aggregate1hCandles() {
        aggregateCandles("1h", 60);
    }
    
    private void aggregateCandles(String interval, int minutes) {
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT");
        
        for (String symbol : symbols) {
            try {
                Instant end = Instant.now().truncatedTo(ChronoUnit.MINUTES);
                Instant start = end.minus(minutes, ChronoUnit.MINUTES);
                
                List<PriceTick> ticks = tickRepository.findBySymbolAndTimestampBetween(
                    symbol, start, end
                );
                
                if (ticks.isEmpty()) continue;
                
                PriceCandle candle = PriceCandle.builder()
                        .symbol(symbol)
                        .interval(interval)
                        .openTime(start)
                        .closeTime(end)
                        .open(ticks.get(0).getPrice())
                        .close(ticks.get(ticks.size() - 1).getPrice())
                        .high(ticks.stream()
                            .map(PriceTick::getPrice)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO))
                        .low(ticks.stream()
                            .map(PriceTick::getPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO))
                        .volume(ticks.stream()
                            .map(PriceTick::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .trades(ticks.size())
                        .build();
                
                candleRepository.save(candle);
                log.debug("Saved {} candle for {}", interval, symbol);
                
            } catch (Exception e) {
                log.error("Failed to aggregate {} candle for {}: {}", interval, symbol, e.getMessage());
            }
        }
    }
    
    @Cacheable(value = "candles", key = "#symbol + '-' + #interval + '-' + #limit")
    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        return candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc(
            symbol.toUpperCase(), 
            interval, 
            limit
        );
    }
}
```

---

## Deployment

### Build and Run with All Services

**Bash (Linux/macOS):**
```bash
# Start all infrastructure
cd docker
docker compose up -d

# Wait for services to be ready
sleep 10

# Verify services
docker compose ps

# Return to project root
cd ..

# Build the application
./mvnw clean package -DskipTests

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**PowerShell (Windows 10/11):**
```powershell
# Start all infrastructure
Set-Location docker
docker compose up -d

# Wait for services to be ready
Start-Sleep -Seconds 10

# Verify services
docker compose ps

# Return to project root
Set-Location ..

# Build the application
.\mvnw.cmd clean package -DskipTests

# Run with dev profile
.\mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=dev"
```

### Verify Services

**Bash (Linux/macOS):**
```bash
# Check MongoDB
docker exec -it trading-mongodb mongosh -u admin -p admin --eval "db.stats()"

# Check Redis
docker exec -it trading-redis redis-cli ping

# Check PostgreSQL
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt"
```

**PowerShell (Windows 10/11):**
```powershell
# Check MongoDB
docker exec -it trading-mongodb mongosh -u admin -p admin --eval "db.stats()"

# Check Redis
docker exec -it trading-redis redis-cli ping

# Check PostgreSQL
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt"
```

---

## Common Pitfalls & Troubleshooting

### Issue 1: MongoDB Authentication Failed

**Symptoms**: `Authentication failed` or `command find requires authentication`

**Solution**: Ensure connection string includes authSource:
```properties
spring.data.mongodb.uri=mongodb://admin:admin@localhost:27017/trading?authSource=admin
```

### Issue 2: Redis Connection Refused

**Symptoms**: `Unable to connect to Redis`

**Solution**: Check Redis is running and port is correct:

**Both Bash and PowerShell:**
```bash
docker logs trading-redis
docker exec -it trading-redis redis-cli ping
```

### Issue 3: WebSocket Handshake Failed

**Symptoms**: `WebSocket connection failed` in browser console

**Solutions**:
1. Check CORS configuration
2. Verify WebSocket endpoint URL
3. Ensure SockJS fallback is enabled

### Issue 4: Binance API Rate Limits

**Symptoms**: `429 Too Many Requests` or connection rejected

**Solutions**:
- Reduce number of symbols
- Implement connection pooling
- Use combined streams: `wss://stream.binance.com:9443/stream?streams=btcusdt@trade/ethusdt@trade`

---

## References

### Official Documentation

- [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Spring WebSocket](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [Binance WebSocket API](https://binance-docs.github.io/apidocs/spot/en/#websocket-market-streams)

### Related Project Documents

- [Architecture.md](../Architecture.md) - System architecture
- [Phase1-ImplementationGuide.md](./Phase1-ImplementationGuide.md) - Previous phase
- [Phase2-TestingGuide.md](./Phase2-TestingGuide.md) - Testing strategies
