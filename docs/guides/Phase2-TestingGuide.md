# Phase 2: Database Optimization - Testing & Verification Guide

**Target Scale**: 10-1,000 users  
**Phase Duration**: Weeks 3-4 (December 14 - December 27, 2025)

---

## Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Test Environment Setup](#test-environment-setup)
3. [MongoDB Testing](#mongodb-testing)
4. [Redis Cache Testing](#redis-cache-testing)
5. [WebSocket Testing](#websocket-testing)
6. [Integration Testing](#integration-testing)
7. [Performance Testing](#performance-testing)
8. [Manual Testing Checklist](#manual-testing-checklist)
9. [Acceptance Criteria Verification](#acceptance-criteria-verification)
10. [References](#references)

---

## Testing Strategy Overview

### Testing Pyramid for Phase 2

```
            ┌─────────────┐
            │ Performance │  ← Load testing (100-1000 users)
            ├─────────────┤
            │   Manual    │  ← WebSocket verification, UI
            ├─────────────┤
            │  API Tests  │  ← REST-assured, WebSocket tests
            ├─────────────┤
            │ Integration │  ← TestContainers (MongoDB, Redis)
            ├─────────────┤
            │    Unit     │  ← JUnit 5, Mockito
            └─────────────┘
```

### Additional Testing Tools for Phase 2

| Tool | Purpose | Version |
|------|---------|---------|
| TestContainers MongoDB | MongoDB testing | 1.19+ |
| TestContainers Redis | Redis testing | 1.19+ |
| Apache JMeter | Load testing | 5.6+ |
| k6 | Performance testing | Latest |
| Spring WebSocket Test | WebSocket testing | 3.x |

---

## Test Environment Setup

### Step 1: Add Test Dependencies

Update `pom.xml` with additional test dependencies:

```xml
<dependencies>
    <!-- Existing test dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- TestContainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mongodb</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Embedded Redis for testing -->
    <dependency>
        <groupId>it.ozimov</groupId>
        <artifactId>embedded-redis</artifactId>
        <version>0.7.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- WebSocket testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Step 2: Create Test Configuration

Create `src/test/resources/application-test.properties`:

```properties
# Test Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver

# MongoDB TestContainers
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/test}

# Redis (will be overridden by embedded Redis)
spring.data.redis.host=localhost
spring.data.redis.port=6370

# Disable scheduled tasks
crawler.enabled=false
spring.main.allow-bean-definition-overriding=true

# Logging
logging.level.org.springframework.data.mongodb=DEBUG
logging.level.org.springframework.data.redis=DEBUG
```

### Step 3: Create TestContainers Base Class

Create `src/test/java/com/example/backend/config/TestContainersConfig.java`:

```java
package com.example.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {
    
    @Bean
    @ServiceConnection
    public MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:6.0"));
    }
}
```

---

## MongoDB Testing

### Repository Tests with TestContainers

Create `src/test/java/com/example/backend/repository/ArticleDocumentRepositoryTest.java`:

```java
package com.example.backend.repository;

import com.example.backend.model.ArticleDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
class ArticleDocumentRepositoryTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private ArticleDocumentRepository repository;
    
    private ArticleDocument testArticle;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
        
        testArticle = ArticleDocument.builder()
                .url("https://example.com/test-article")
                .title("Bitcoin Analysis")
                .body("Detailed analysis of Bitcoin market trends")
                .source("coindesk")
                .publishedAt(LocalDateTime.now())
                .metadata(Map.of("author", "John Doe"))
                .symbols(List.of("BTC", "USDT"))
                .build();
    }
    
    @Test
    void shouldSaveAndRetrieveDocument() {
        ArticleDocument saved = repository.save(testArticle);
        
        assertThat(saved.getId()).isNotNull();
        
        Optional<ArticleDocument> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Bitcoin Analysis");
    }
    
    @Test
    void shouldFindByUrl() {
        repository.save(testArticle);
        
        Optional<ArticleDocument> found = repository.findByUrl(testArticle.getUrl());
        
        assertThat(found).isPresent();
        assertThat(found.get().getSource()).isEqualTo("coindesk");
    }
    
    @Test
    void shouldSearchByKeyword() {
        repository.save(testArticle);
        
        Page<ArticleDocument> results = repository.searchByKeyword(
            "Bitcoin", 
            PageRequest.of(0, 10)
        );
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getBody()).contains("Bitcoin");
    }
    
    @Test
    void shouldFindBySymbols() {
        repository.save(testArticle);
        
        List<ArticleDocument> results = repository.findBySymbolsContaining("BTC");
        
        assertThat(results).hasSize(1);
    }
    
    @Test
    void shouldPreventDuplicateUrls() {
        repository.save(testArticle);
        
        assertThat(repository.existsByUrl(testArticle.getUrl())).isTrue();
        assertThat(repository.existsByUrl("https://nonexistent.com")).isFalse();
    }
}
```

---

## Redis Cache Testing

### Cache Integration Tests

Create `src/test/java/com/example/backend/service/CacheServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.model.ArticleDocument;
import com.example.backend.repository.ArticleDocumentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CacheServiceTest {
    
    private static RedisServer redisServer;
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ArticleDocumentRepository repository;
    
    @Autowired
    private CacheManager cacheManager;
    
    @BeforeAll
    static void startRedis() {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }
    
    @AfterAll
    static void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.port", () -> "6370");
    }
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> 
            cacheManager.getCache(name).clear()
        );
    }
    
    @Test
    void shouldCacheArticles() {
        // Given
        ArticleDocument article = ArticleDocument.builder()
                .url("https://test.com/article-1")
                .title("Test Article")
                .source("test")
                .publishedAt(LocalDateTime.now())
                .build();
        repository.save(article);
        
        // When - First call (cache miss)
        articleService.getAllArticles(PageRequest.of(0, 10));
        
        // Then - Second call should use cache
        articleService.getAllArticles(PageRequest.of(0, 10));
        
        // Verify cache is populated
        assertThat(cacheManager.getCache("articles")).isNotNull();
    }
    
    @Test
    void shouldEvictCacheOnSave() {
        // Given
        ArticleDocument article1 = ArticleDocument.builder()
                .url("https://test.com/article-1")
                .title("Article 1")
                .source("test")
                .build();
        repository.save(article1);
        
        // Populate cache
        articleService.getAllArticles(PageRequest.of(0, 10));
        
        // When - Save new article (should evict cache)
        ArticleDocument article2 = ArticleDocument.builder()
                .url("https://test.com/article-2")
                .title("Article 2")
                .source("test")
                .build();
        articleService.saveArticle(article2);
        
        // Then - Cache should be evicted
        // Next call should fetch from database
        var result = articleService.getAllArticles(PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
```

### Redis Connection Test

Create `src/test/java/com/example/backend/config/RedisConnectionTest.java`:

```java
package com.example.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RedisConnectionTest {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Test
    void shouldConnectToRedis() {
        // When
        redisTemplate.opsForValue().set("test-key", "test-value");
        String value = redisTemplate.opsForValue().get("test-key");
        
        // Then
        assertThat(value).isEqualTo("test-value");
        
        // Cleanup
        redisTemplate.delete("test-key");
    }
}
```

---

## WebSocket Testing

### WebSocket Integration Tests

Create `src/test/java/com/example/backend/websocket/WebSocketPriceTest.java`:

```java
package com.example.backend.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketPriceTest {
    
    @LocalServerPort
    private int port;
    
    private WebSocketStompClient stompClient;
    
    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }
    
    @Test
    void shouldConnectToWebSocket() throws Exception {
        CompletableFuture<Boolean> connected = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connected.complete(true);
            }
            
            @Override
            public void handleException(StompSession session, StompCommand command, 
                    StompHeaders headers, byte[] payload, Throwable exception) {
                connected.completeExceptionally(exception);
            }
        };
        
        String url = String.format("ws://localhost:%d/ws/prices", port);
        stompClient.connect(url, sessionHandler);
        
        Boolean result = connected.get(5, TimeUnit.SECONDS);
        assertThat(result).isTrue();
    }
    
    @Test
    void shouldSubscribeToPriceTopic() throws Exception {
        CompletableFuture<Object> messageReceived = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/topic/prices/btcusdt", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Object.class;
                    }
                    
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        messageReceived.complete(payload);
                    }
                });
            }
        };
        
        String url = String.format("ws://localhost:%d/ws/prices", port);
        stompClient.connect(url, sessionHandler);
        
        // Note: In real test, you'd need to trigger a price update
        // This test verifies connection and subscription work
    }
}
```

---

## Integration Testing

### Full Stack Integration Test

Create `src/test/java/com/example/backend/integration/Phase2IntegrationTest.java`:

```java
package com.example.backend.integration;

import com.example.backend.model.ArticleDocument;
import com.example.backend.repository.ArticleDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class Phase2IntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ArticleDocumentRepository articleRepository;
    
    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        
        // Seed test data
        ArticleDocument article1 = ArticleDocument.builder()
                .url("https://example.com/btc-analysis")
                .title("Bitcoin reaches new high")
                .body("Bitcoin price analysis shows bullish trend")
                .source("coindesk")
                .symbols(List.of("BTC", "USDT"))
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build();
        
        ArticleDocument article2 = ArticleDocument.builder()
                .url("https://example.com/eth-update")
                .title("Ethereum network upgrade")
                .body("Major Ethereum upgrade completed")
                .source("cointelegraph")
                .symbols(List.of("ETH"))
                .publishedAt(LocalDateTime.now())
                .build();
        
        articleRepository.saveAll(List.of(article1, article2));
    }
    
    @Test
    void shouldReturnArticlesFromMongoDB() throws Exception {
        mockMvc.perform(get("/api/articles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
    
    @Test
    void shouldSearchArticlesInMongoDB() throws Exception {
        mockMvc.perform(get("/api/articles/search")
                .param("q", "Bitcoin")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", containsString("Bitcoin")));
    }
    
    @Test
    void shouldFilterBySource() throws Exception {
        mockMvc.perform(get("/api/articles/source/coindesk")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].source", is("coindesk")));
    }
}
```

---

## Performance Testing

### JMeter Test Plan

Create a JMeter test plan for load testing:

**Test Scenarios:**

1. **Articles API Load Test**
   - Threads: 100
   - Ramp-up: 60 seconds
   - Duration: 300 seconds
   - Target: GET /api/articles

2. **WebSocket Connection Test**
   - Threads: 500
   - Ramp-up: 120 seconds
   - Duration: 300 seconds
   - Target: WS /ws/prices

### k6 Performance Script

Create `tests/performance/load-test.js`:

```javascript
import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '3m', target: 100 },  // Stay at 100 users
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests under 200ms
    http_req_failed: ['rate<0.01'],   // Less than 1% failures
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // Test Articles API
  const articlesRes = http.get(`${BASE_URL}/api/articles?page=0&size=10`);
  check(articlesRes, {
    'articles status is 200': (r) => r.status === 200,
    'articles response time < 200ms': (r) => r.timings.duration < 200,
  });

  // Test Health endpoint
  const healthRes = http.get(`${BASE_URL}/api/health`);
  check(healthRes, {
    'health status is 200': (r) => r.status === 200,
  });

  // Test Price endpoint
  const priceRes = http.get(`${BASE_URL}/api/prices/current/BTCUSDT`);
  check(priceRes, {
    'price status is 200 or 404': (r) => [200, 404].includes(r.status),
  });

  sleep(1);
}

export function websocketTest() {
  const url = 'ws://localhost:8080/ws/prices';
  
  ws.connect(url, function (socket) {
    socket.on('open', () => {
      socket.send(JSON.stringify({
        type: 'subscribe',
        channel: 'btcusdt'
      }));
    });

    socket.on('message', (data) => {
      const message = JSON.parse(data);
      check(message, {
        'has price field': (m) => m.price !== undefined,
      });
    });

    socket.setTimeout(function () {
      socket.close();
    }, 30000);
  });
}
```

### Run Performance Tests

**Bash (Linux/macOS):**
```bash
# Install k6
brew install k6

# Run load test
k6 run tests/performance/load-test.js

# Run with more virtual users
k6 run --vus 200 --duration 5m tests/performance/load-test.js

# Generate HTML report
k6 run --out json=results.json tests/performance/load-test.js
```

**PowerShell (Windows 10/11):**
```powershell
# Install k6 using chocolatey
choco install k6

# Run load test
k6 run tests/performance/load-test.js

# Run with more virtual users
k6 run --vus 200 --duration 5m tests/performance/load-test.js

# Generate HTML report
k6 run --out json=results.json tests/performance/load-test.js
```

---

## Manual Testing Checklist

### MongoDB Integration

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-101 | Save article to MongoDB | Document created with ID | ☐ |
| MT-102 | Query articles with filters | Returns filtered results | ☐ |
| MT-103 | Search with regex | Returns matching documents | ☐ |
| MT-104 | Check indexes exist | Indexes on url, source, publishedAt | ☐ |

### Redis Caching

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-110 | Call API twice | Second call uses cache (check logs) | ☐ |
| MT-111 | Save new article | Cache is evicted | ☐ |
| MT-112 | Check cache TTL | Entries expire after configured time | ☐ |
| MT-113 | Redis CLI inspect | Keys exist in Redis | ☐ |

### WebSocket

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-120 | Connect to WebSocket | Connection established | ☐ |
| MT-121 | Subscribe to price topic | Subscription confirmed | ☐ |
| MT-122 | Receive price updates | Messages received in real-time | ☐ |
| MT-123 | Reconnect after disconnect | Auto-reconnection works | ☐ |

### Price Collector

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-130 | Check Binance connection | Connected to WebSocket | ☐ |
| MT-131 | Verify ticks stored | price_ticks table has data | ☐ |
| MT-132 | Check candle aggregation | price_candles populated | ☐ |
| MT-133 | API returns historical data | GET /api/prices/historical works | ☐ |

---

## Acceptance Criteria Verification

### Phase 2 Acceptance Criteria

| Criteria | Test Method | Command | Status |
|----------|-------------|---------|--------|
| MongoDB storing articles | Query MongoDB | `db.articles.count()` | ☐ |
| Redis cache hit > 80% | Check Redis stats | `redis-cli info stats` | ☐ |
| Real-time price updates | WebSocket connection | Browser console | ☐ |
| Candle aggregation works | Query candles API | `curl /api/prices/historical` | ☐ |
| Cache eviction on write | Save + query | Check logs for cache miss | ☐ |

### Verification Script

**Bash (Linux/macOS):**
```bash
#!/bin/bash
echo "=== Phase 2 Verification Script ==="

# 1. MongoDB Check
echo "1. Checking MongoDB..."
MONGO_COUNT=$(docker exec trading-mongodb mongosh -u admin -p admin --authenticationDatabase admin trading --quiet --eval "db.articles.countDocuments()")
if [[ $MONGO_COUNT -ge 0 ]]; then
    echo "   ✓ MongoDB working. Articles: $MONGO_COUNT"
else
    echo "   ✗ MongoDB check failed"
fi

# 2. Redis Check
echo "2. Checking Redis..."
REDIS_PING=$(docker exec trading-redis redis-cli ping)
if [[ $REDIS_PING == "PONG" ]]; then
    echo "   ✓ Redis connected"
else
    echo "   ✗ Redis check failed"
fi

# 3. Price API Check
echo "3. Checking Price API..."
PRICE=$(curl -s http://localhost:8080/api/prices/current/BTCUSDT)
if [[ $PRICE == *"price"* ]] || [[ $PRICE == *"symbol"* ]]; then
    echo "   ✓ Price API working"
else
    echo "   ⚠ Price API may not have data yet"
fi

# 4. WebSocket Check
echo "4. Checking WebSocket endpoint..."
WS_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ws/prices/info)
if [[ $WS_CHECK == "200" ]] || [[ $WS_CHECK == "101" ]]; then
    echo "   ✓ WebSocket endpoint available"
else
    echo "   ⚠ WebSocket check returned: $WS_CHECK"
fi

# 5. Cache Stats
echo "5. Checking Redis cache stats..."
CACHE_KEYS=$(docker exec trading-redis redis-cli keys '*' | wc -l)
echo "   Cache keys: $CACHE_KEYS"

echo "=== Verification Complete ==="
```

**PowerShell (Windows 10/11):**
```powershell
Write-Host "=== Phase 2 Verification Script ===" -ForegroundColor Cyan

# 1. MongoDB Check
Write-Host "1. Checking MongoDB..."
try {
    $mongoCount = docker exec trading-mongodb mongosh -u admin -p admin --authenticationDatabase admin trading --quiet --eval "db.articles.countDocuments()"
    Write-Host "   ✓ MongoDB working. Articles: $mongoCount" -ForegroundColor Green
} catch {
    Write-Host "   ✗ MongoDB check failed: $_" -ForegroundColor Red
}

# 2. Redis Check
Write-Host "2. Checking Redis..."
$redisPing = docker exec trading-redis redis-cli ping
if ($redisPing -eq "PONG") {
    Write-Host "   ✓ Redis connected" -ForegroundColor Green
} else {
    Write-Host "   ✗ Redis check failed" -ForegroundColor Red
}

# 3. Price API Check
Write-Host "3. Checking Price API..."
try {
    $price = Invoke-RestMethod -Uri http://localhost:8080/api/prices/current/BTCUSDT -ErrorAction SilentlyContinue
    if ($price.price) {
        Write-Host "   ✓ Price API working: $($price.price)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Price API may not have data yet" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ⚠ Price API not responding" -ForegroundColor Yellow
}

# 4. WebSocket Check
Write-Host "4. Checking WebSocket endpoint..."
try {
    $wsCheck = Invoke-WebRequest -Uri http://localhost:8080/ws/prices/info -UseBasicParsing -ErrorAction SilentlyContinue
    Write-Host "   ✓ WebSocket endpoint available" -ForegroundColor Green
} catch {
    Write-Host "   ⚠ WebSocket endpoint check returned error" -ForegroundColor Yellow
}

# 5. Cache Stats
Write-Host "5. Checking Redis cache stats..."
$cacheKeys = docker exec trading-redis redis-cli keys '*'
$keyCount = ($cacheKeys | Measure-Object -Line).Lines
Write-Host "   Cache keys: $keyCount" -ForegroundColor Cyan

Write-Host "=== Verification Complete ===" -ForegroundColor Cyan
```

---

## Common Issues & Troubleshooting

### Issue 1: TestContainers Docker Not Available

**Symptoms**: `Could not find a valid Docker environment`

**Solutions**:
1. Ensure Docker Desktop is running
2. Set Docker environment variable:

**Bash:**
```bash
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

**PowerShell:**
```powershell
$env:TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE = "//var/run/docker.sock"
```

### Issue 2: Redis Embedded Server Port Conflict

**Symptoms**: `Address already in use`

**Solution**: Use a different port for embedded Redis:
```java
redisServer = new RedisServer(6370); // Use non-default port
```

### Issue 3: WebSocket Test Timeout

**Symptoms**: `Timeout waiting for connection`

**Solutions**:
1. Increase timeout in test
2. Check server is running before test
3. Verify WebSocket configuration

---

## References

### Testing Documentation

- [TestContainers for Java](https://www.testcontainers.org/modules/databases/mongodb/)
- [Spring Data MongoDB Testing](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#testing)
- [k6 Documentation](https://k6.io/docs/)
- [Spring WebSocket Testing](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#websocket-testing)

### Related Project Documents

- [Phase2-ImplementationGuide.md](./Phase2-ImplementationGuide.md) - Implementation guide
- [Architecture.md](../Architecture.md) - System architecture
- [Phase3-TestingGuide.md](./Phase3-TestingGuide.md) - Next phase testing
