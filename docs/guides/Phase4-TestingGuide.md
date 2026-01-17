# Phase 4: Async Processing - Testing & Verification Guide

**Target Scale**: 1,000-10,000 users  
**Phase Duration**: Weeks 7-8 (January 11 - January 24, 2026)

---

## Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Kafka Testing](#kafka-testing)
3. [Consumer Testing](#consumer-testing)
4. [Load Testing](#load-testing)
5. [End-to-End Testing](#end-to-end-testing)
6. [Manual Testing Checklist](#manual-testing-checklist)
7. [Acceptance Criteria Verification](#acceptance-criteria-verification)
8. [References](#references)

---

## Testing Strategy Overview

### Testing Focus for Phase 4

| Component | Test Types | Priority |
|-----------|------------|----------|
| Kafka Producers | Message publishing, serialization | High |
| Kafka Consumers | Message processing, error handling | High |
| Redis Pub/Sub | Real-time message delivery | High |
| Load Testing | 1000+ concurrent connections | High |

### Testing Tools

| Tool | Purpose | Version |
|------|---------|---------|
| EmbeddedKafka | Kafka unit testing | Spring Kafka |
| TestContainers Kafka | Integration testing | 1.19+ |
| k6 | Load testing | Latest |
| Gatling | Performance testing | 3.9+ |

---

## Kafka Testing

### Producer Tests

Create `src/test/java/com/example/backend/crawler/CrawlerKafkaProducerTest.java`:

```java
package com.example.backend.crawler;

import com.example.backend.event.ArticleCrawledEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"raw-articles"})
@ActiveProfiles("test")
@DirtiesContext
class CrawlerKafkaProducerTest {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Test
    void shouldPublishArticleEvent() throws Exception {
        // Given
        ArticleCrawledEvent event = ArticleCrawledEvent.builder()
                .eventId("test-event-1")
                .url("https://test.com/article")
                .title("Test Article")
                .source("test-source")
                .crawledAt(LocalDateTime.now())
                .metadata(Map.of("test", "value"))
                .build();
        
        // When
        kafkaTemplate.send("raw-articles", event.getUrl(), event).get(10, TimeUnit.SECONDS);
        
        // Then - verify message was sent (would normally check consumer in integration test)
        assertThat(event.getEventId()).isNotNull();
    }
}
```

### Consumer Tests

Create `src/test/java/com/example/backend/worker/NormalizerWorkerTest.java`:

```java
package com.example.backend.worker;

import com.example.backend.event.ArticleCrawledEvent;
import com.example.backend.model.ArticleDocument;
import com.example.backend.repository.mongodb.ArticleDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NormalizerWorkerTest {

    @Mock
    private ArticleDocumentRepository articleRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private NormalizerWorker normalizerWorker;

    private ArticleCrawledEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = ArticleCrawledEvent.builder()
                .eventId("test-event")
                .url("https://test.com/new-article")
                .title("Test Article")
                .rawHtml("<html><body>Test content</body></html>")
                .source("test-source")
                .crawledAt(LocalDateTime.now())
                .metadata(Map.of("test", "value"))
                .build();
    }

    @Test
    void shouldNormalizeAndSaveArticle() {
        // Given
        when(articleRepository.existsByUrl(testEvent.getUrl())).thenReturn(false);
        when(articleRepository.save(any(ArticleDocument.class)))
                .thenAnswer(i -> {
                    ArticleDocument doc = i.getArgument(0);
                    doc.setId("generated-id");
                    return doc;
                });

        // When
        normalizerWorker.processRawArticle(testEvent);

        // Then
        ArgumentCaptor<ArticleDocument> captor = ArgumentCaptor.forClass(ArticleDocument.class);
        verify(articleRepository).save(captor.capture());

        ArticleDocument saved = captor.getValue();
        assertThat(saved.getUrl()).isEqualTo(testEvent.getUrl());
        assertThat(saved.getTitle()).isEqualTo(testEvent.getTitle());
        assertThat(saved.getBody()).contains("Test content");
    }

    @Test
    void shouldSkipDuplicateArticle() {
        // Given
        when(articleRepository.existsByUrl(testEvent.getUrl())).thenReturn(true);

        // When
        normalizerWorker.processRawArticle(testEvent);

        // Then
        verify(articleRepository, never()).save(any());
    }

    @Test
    void shouldPublishNormalizedEvent() {
        // Given
        when(articleRepository.existsByUrl(testEvent.getUrl())).thenReturn(false);
        when(articleRepository.save(any(ArticleDocument.class)))
                .thenAnswer(i -> {
                    ArticleDocument doc = i.getArgument(0);
                    doc.setId("generated-id");
                    return doc;
                });

        // When
        normalizerWorker.processRawArticle(testEvent);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }
}
```

### NLP Worker Tests

Create `src/test/java/com/example/backend/worker/NlpWorkerTest.java`:

```java
package com.example.backend.worker;

import com.example.backend.event.ArticleNormalizedEvent;
import com.example.backend.model.ArticleDocument;
import com.example.backend.nlp.SentimentAnalysisService;
import com.example.backend.repository.mongodb.ArticleDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NlpWorkerTest {

    @Mock
    private ArticleDocumentRepository articleRepository;

    @Mock
    private SentimentAnalysisService sentimentService;

    @InjectMocks
    private NlpWorker nlpWorker;

    @Test
    void shouldProcessArticleWithNlp() {
        // Given
        ArticleNormalizedEvent event = ArticleNormalizedEvent.builder()
                .eventId("event-1")
                .articleId("article-1")
                .title("Bitcoin bullish momentum")
                .body("Bitcoin shows strong bullish signals")
                .source("test")
                .publishedAt(LocalDateTime.now())
                .build();

        ArticleDocument article = ArticleDocument.builder()
                .id("article-1")
                .title("Bitcoin bullish momentum")
                .body("Bitcoin shows strong bullish signals")
                .build();

        ArticleDocument.SentimentResult sentiment = ArticleDocument.SentimentResult.builder()
                .label("bullish")
                .score(0.8)
                .build();

        when(articleRepository.findById("article-1")).thenReturn(Optional.of(article));
        when(sentimentService.analyzeSentiment(anyString())).thenReturn(sentiment);
        when(sentimentService.extractSymbols(anyString())).thenReturn(List.of("BTC"));
        when(sentimentService.extractEntities(anyString())).thenReturn(List.of("Bitcoin"));

        // When
        nlpWorker.processNormalizedArticle(event);

        // Then
        verify(sentimentService).analyzeSentiment(article.getBody());
        verify(sentimentService).extractSymbols(article.getBody());
        verify(articleRepository).save(article);
    }

    @Test
    void shouldHandleArticleNotFound() {
        // Given
        ArticleNormalizedEvent event = ArticleNormalizedEvent.builder()
                .eventId("event-1")
                .articleId("non-existent")
                .build();

        when(articleRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When
        nlpWorker.processNormalizedArticle(event);

        // Then
        verify(sentimentService, never()).analyzeSentiment(anyString());
    }
}
```

---

## Integration Testing with TestContainers

### Kafka Integration Test

Create `src/test/java/com/example/backend/integration/KafkaIntegrationTest.java`:

```java
package com.example.backend.integration;

import com.example.backend.event.ArticleCrawledEvent;
import com.example.backend.repository.mongodb.ArticleDocumentRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ArticleDocumentRepository articleRepository;

    @Test
    void shouldProcessArticleThroughKafkaPipeline() throws Exception {
        // Given
        ArticleCrawledEvent event = ArticleCrawledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .url("https://test.com/kafka-test-" + System.currentTimeMillis())
                .title("Kafka Integration Test Article")
                .rawHtml("<html><body>Test content for Kafka</body></html>")
                .source("test")
                .crawledAt(LocalDateTime.now())
                .metadata(Map.of("test", "kafka"))
                .build();

        // When
        kafkaTemplate.send("raw-articles", event.getUrl(), event).get(10, TimeUnit.SECONDS);

        // Then - wait for consumer to process
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(articleRepository.existsByUrl(event.getUrl())).isTrue();
        });
    }
}
```

---

## Load Testing

### Extended k6 Test Script

Create `tests/performance/load-test-phase4-full.js`:

```javascript
import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const wsConnectionTime = new Trend('ws_connection_time');
const wsMessagesReceived = new Counter('ws_messages_received');

export const options = {
  scenarios: {
    // API Load Test
    api_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 500 },
        { duration: '5m', target: 1000 },
        { duration: '3m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    // WebSocket Stress Test
    websocket_stress: {
      executor: 'constant-vus',
      vus: 500,
      duration: '10m',
      exec: 'websocketTest',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.05'],
    ws_connection_time: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  group('API Tests', () => {
    // Health Check
    const healthRes = http.get(`${BASE_URL}/api/health`);
    check(healthRes, {
      'health status is 200': (r) => r.status === 200,
    });

    // Articles API
    const articlesRes = http.get(`${BASE_URL}/api/articles?page=0&size=10`);
    check(articlesRes, {
      'articles status is 200': (r) => r.status === 200,
      'articles has content': (r) => JSON.parse(r.body).content !== undefined,
    }) || errorRate.add(1);

    // Price Historical
    const priceRes = http.get(
      `${BASE_URL}/api/prices/historical?symbol=BTCUSDT&interval=1h&limit=100`
    );
    check(priceRes, {
      'prices status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    // Analysis Endpoint
    const analysisRes = http.get(`${BASE_URL}/api/analysis/sentiment/bullish`);
    check(analysisRes, {
      'analysis returns data': (r) => r.status === 200,
    });
  });

  sleep(Math.random() * 2 + 1);
}

export function websocketTest() {
  const startTime = Date.now();
  const url = `ws://${__ENV.WS_HOST || 'localhost:8080'}/ws/prices`;

  const res = ws.connect(url, {}, function (socket) {
    wsConnectionTime.add(Date.now() - startTime);

    socket.on('open', () => {
      socket.send(JSON.stringify({
        type: 'subscribe',
        channel: 'btcusdt'
      }));
    });

    socket.on('message', (data) => {
      wsMessagesReceived.add(1);
      check(JSON.parse(data), {
        'message has price': (m) => m.price !== undefined,
      });
    });

    socket.on('error', (e) => {
      errorRate.add(1);
      console.error('WebSocket error:', e);
    });

    socket.setTimeout(function () {
      socket.close();
    }, 60000);
  });

  check(res, {
    'ws connected': (r) => r && r.status === 101,
  }) || errorRate.add(1);
}
```

### Run Load Tests

**Bash (Linux/macOS):**
```bash
# Install k6 if not installed
brew install k6

# Run full load test
k6 run tests/performance/load-test-phase4-full.js

# Run with specific targets
k6 run --env BASE_URL=http://localhost:8080 --env WS_HOST=localhost:8080 \
  tests/performance/load-test-phase4-full.js

# Generate HTML report
k6 run --out json=results.json tests/performance/load-test-phase4-full.js
```

**PowerShell (Windows 10/11):**
```powershell
# Install k6 if not installed
choco install k6

# Run full load test
k6 run tests/performance/load-test-phase4-full.js

# Run with specific targets
k6 run --env BASE_URL=http://localhost:8080 --env WS_HOST=localhost:8080 `
  tests/performance/load-test-phase4-full.js

# Generate HTML report
k6 run --out json=results.json tests/performance/load-test-phase4-full.js
```

---

## Manual Testing Checklist

### Kafka Infrastructure

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-401 | List Kafka topics | Shows raw-articles, normalized-articles, price-ticks | ☐ |
| MT-402 | Check consumer groups | Shows active consumer groups | ☐ |
| MT-403 | Monitor consumer lag | Lag should be near 0 | ☐ |
| MT-404 | Access Kafka UI | UI accessible at port 8090 | ☐ |

### Message Flow

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-410 | Trigger crawler | Messages appear in raw-articles topic | ☐ |
| MT-411 | Check normalizer | Messages appear in normalized-articles | ☐ |
| MT-412 | Verify NLP processing | Articles have sentiment in MongoDB | ☐ |
| MT-413 | Check price ticks | Price messages in price-ticks topic | ☐ |

### Load Testing Results

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-420 | 100 concurrent users | p95 latency < 200ms | ☐ |
| MT-421 | 500 concurrent users | p95 latency < 500ms | ☐ |
| MT-422 | 1000 concurrent users | Error rate < 1% | ☐ |
| MT-423 | 500 WebSocket connections | All connected, receiving messages | ☐ |

---

## Acceptance Criteria Verification

### Phase 4 Acceptance Criteria

| Criteria | Test Method | Command | Status |
|----------|-------------|---------|--------|
| Kafka pipeline processing | Check topics | `kafka-topics --list` | ☐ |
| 1000 WebSocket connections | Load test | k6 websocket test | ☐ |
| No data loss on restart | Kill worker, verify | Restart consumer, check lag | ☐ |
| Consumer lag < 100 | Monitor | `kafka-consumer-groups --describe` | ☐ |

### Verification Script

**Bash (Linux/macOS):**
```bash
#!/bin/bash
echo "=== Phase 4 Verification Script ==="

# 1. Kafka Topics
echo "1. Checking Kafka topics..."
TOPICS=$(docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null)
if [[ $TOPICS == *"raw-articles"* ]] && [[ $TOPICS == *"normalized-articles"* ]]; then
    echo "   ✓ Kafka topics exist"
else
    echo "   ✗ Kafka topics missing"
fi

# 2. Consumer Groups
echo "2. Checking consumer groups..."
GROUPS=$(docker exec trading-kafka kafka-consumer-groups --list --bootstrap-server localhost:9092 2>/dev/null)
if [[ ! -z "$GROUPS" ]]; then
    echo "   ✓ Consumer groups active: $GROUPS"
else
    echo "   ⚠ No consumer groups found"
fi

# 3. Consumer Lag
echo "3. Checking consumer lag..."
LAG=$(docker exec trading-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group trading-platform 2>/dev/null | tail -n +2)
echo "   Consumer lag info:"
echo "$LAG"

# 4. Kafka UI
echo "4. Checking Kafka UI..."
KAFKA_UI=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090)
if [[ $KAFKA_UI == "200" ]]; then
    echo "   ✓ Kafka UI accessible"
else
    echo "   ✗ Kafka UI not accessible"
fi

# 5. Message Count
echo "5. Checking message counts..."
for topic in raw-articles normalized-articles price-ticks; do
    COUNT=$(docker exec trading-kafka kafka-run-class kafka.tools.GetOffsetShell \
      --broker-list localhost:9092 --topic $topic 2>/dev/null | awk -F: '{sum += $3} END {print sum}')
    echo "   $topic: $COUNT messages"
done

echo "=== Verification Complete ==="
```

**PowerShell (Windows 10/11):**
```powershell
Write-Host "=== Phase 4 Verification Script ===" -ForegroundColor Cyan

# 1. Kafka Topics
Write-Host "1. Checking Kafka topics..."
$topics = docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092 2>$null
if ($topics -match "raw-articles" -and $topics -match "normalized-articles") {
    Write-Host "   ✓ Kafka topics exist" -ForegroundColor Green
} else {
    Write-Host "   ✗ Kafka topics missing" -ForegroundColor Red
}

# 2. Consumer Groups
Write-Host "2. Checking consumer groups..."
$groups = docker exec trading-kafka kafka-consumer-groups --list --bootstrap-server localhost:9092 2>$null
if ($groups) {
    Write-Host "   ✓ Consumer groups active: $groups" -ForegroundColor Green
} else {
    Write-Host "   ⚠ No consumer groups found" -ForegroundColor Yellow
}

# 3. Kafka UI
Write-Host "3. Checking Kafka UI..."
try {
    $response = Invoke-WebRequest -Uri http://localhost:8090 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✓ Kafka UI accessible" -ForegroundColor Green
    }
} catch {
    Write-Host "   ✗ Kafka UI not accessible" -ForegroundColor Red
}

Write-Host "=== Verification Complete ===" -ForegroundColor Cyan
```

---

## References

### Testing Documentation

- [Spring Kafka Testing](https://docs.spring.io/spring-kafka/docs/current/reference/html/#testing)
- [TestContainers Kafka](https://www.testcontainers.org/modules/kafka/)
- [k6 Documentation](https://k6.io/docs/)
- [Awaitility](https://github.com/awaitility/awaitility)

### Related Project Documents

- [Phase4-ImplementationGuide.md](./Phase4-ImplementationGuide.md) - Implementation guide
- [Architecture.md](../Architecture.md) - System architecture
- [Phase5-ImplementationGuide.md](./Phase5-ImplementationGuide.md) - Next phase
