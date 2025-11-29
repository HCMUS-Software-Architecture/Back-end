# Phase 4: Async Processing - Implementation Guide

**Target Scale**: 1,000-10,000 users  
**Architecture**: Event-driven with Apache Kafka  
**Duration**: Weeks 7-8 (January 11 - January 24, 2026)

---

## Table of Contents

1. [Overview](#overview)
2. [Kafka Setup](#kafka-setup)
3. [Event-Driven Architecture](#event-driven-architecture)
4. [Producer Implementation](#producer-implementation)
5. [Consumer Implementation](#consumer-implementation)
6. [Redis Pub/Sub for WebSocket](#redis-pubsub-for-websocket)
7. [Load Testing](#load-testing)
8. [Deployment](#deployment)
9. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
10. [References](#references)

---

## Overview

### Goals

- Integrate Apache Kafka for event streaming
- Refactor Crawler to publish to Kafka
- Create NLP Worker as Kafka consumer
- Implement price aggregation via Kafka
- Use Redis Pub/Sub for WebSocket scaling
- Load test for 1000+ concurrent connections

### Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                          API Gateway                              │
└───────────────────────────────┬──────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────────┐
        │                       │                           │
  ┌─────┴─────┐           ┌─────┴─────┐             ┌───────┴──────┐
  │    API    │           │  Crawler  │             │    Price     │
  │  Service  │           │  Service  │             │   Service    │
  └─────┬─────┘           └─────┬─────┘             └───────┬──────┘
        │                       │                           │
        │                       └───────────┬───────────────┘
        │                                   │
        │                           ┌───────┴───────┐
        │                           │     Kafka     │
        │                           └───────┬───────┘
        │                                   │
        │                           ┌───────┴───────┐
        │                           │  NLP Worker   │
        │                           │  (Consumer)   │
        │                           └───────────────┘
        │
┌───────┴─────────────────────────────────────────┐
│               Data Layer                         │
│  PostgreSQL    │    MongoDB    │    Redis        │
└──────────────────────────────────────────────────┘
```

### Epics for Phase 4

| Epic | Description | Priority |
|------|-------------|----------|
| E4.1 | Kafka Infrastructure Setup | High |
| E4.2 | Crawler → Kafka Producer | High |
| E4.3 | NLP Worker as Kafka Consumer | High |
| E4.4 | Price Aggregator Consumer | High |
| E4.5 | Redis Pub/Sub for WebSocket | Medium |
| E4.6 | Load Testing Infrastructure | Medium |

---

## Kafka Setup

### Step 1: Update Docker Compose

Update `docker/docker-compose.yml`:

```yaml
version: '3.8'

services:
  # Existing services...
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

  mongodb:
    image: mongo:6.0
    container_name: trading-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db

  redis:
    image: redis:7-alpine
    container_name: trading-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes

  # Kafka Infrastructure
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: trading-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    volumes:
      - zk_data:/var/lib/zookeeper/data
      - zk_logs:/var/lib/zookeeper/log

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: trading-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    volumes:
      - kafka_data:/var/lib/kafka/data

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: trading-kafka-ui
    depends_on:
      - kafka
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: trading-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181

volumes:
  postgres_data:
  mongo_data:
  redis_data:
  zk_data:
  zk_logs:
  kafka_data:
```

### Step 2: Start Kafka

**Bash (Linux/macOS):**
```bash
cd docker
docker compose up -d

# Wait for Kafka to be ready
sleep 30

# Create topics
docker exec trading-kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic raw-articles

docker exec trading-kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic normalized-articles

docker exec trading-kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 6 \
  --topic price-ticks

# List topics
docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location docker
docker compose up -d

# Wait for Kafka to be ready
Start-Sleep -Seconds 30

# Create topics
docker exec trading-kafka kafka-topics --create `
  --bootstrap-server localhost:9092 `
  --replication-factor 1 `
  --partitions 3 `
  --topic raw-articles

docker exec trading-kafka kafka-topics --create `
  --bootstrap-server localhost:9092 `
  --replication-factor 1 `
  --partitions 3 `
  --topic normalized-articles

docker exec trading-kafka kafka-topics --create `
  --bootstrap-server localhost:9092 `
  --replication-factor 1 `
  --partitions 6 `
  --topic price-ticks

# List topics
docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Step 3: Add Kafka Dependencies

Update `pom.xml`:

```xml
<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Step 4: Kafka Configuration

Update `src/main/resources/application-dev.properties`:

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=trading-platform
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Topic names
kafka.topic.raw-articles=raw-articles
kafka.topic.normalized-articles=normalized-articles
kafka.topic.price-ticks=price-ticks
```

---

## Event-Driven Architecture

### Event DTOs

Create `src/main/java/com/example/backend/event/ArticleCrawledEvent.java`:

```java
package com.example.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCrawledEvent {
    private String eventId;
    private String url;
    private String title;
    private String rawHtml;
    private String source;
    private LocalDateTime crawledAt;
    private Map<String, Object> metadata;
}
```

Create `src/main/java/com/example/backend/event/ArticleNormalizedEvent.java`:

```java
package com.example.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleNormalizedEvent {
    private String eventId;
    private String articleId;
    private String title;
    private String body;
    private String source;
    private LocalDateTime publishedAt;
    private List<String> symbols;
}
```

Create `src/main/java/com/example/backend/event/PriceTickEvent.java`:

```java
package com.example.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceTickEvent {
    private String symbol;
    private BigDecimal price;
    private BigDecimal quantity;
    private Instant timestamp;
    private String exchange;
}
```

---

## Producer Implementation

### Kafka Producer Configuration

Create `src/main/java/com/example/backend/config/KafkaProducerConfig.java`:

```java
package com.example.backend.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Crawler as Producer

Update `src/main/java/com/example/backend/crawler/CrawlerService.java`:

```java
package com.example.backend.crawler;

import com.example.backend.event.ArticleCrawledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.raw-articles}")
    private String rawArticlesTopic;
    
    private static final Map<String, SourceConfig> SOURCES = Map.of(
        "coindesk", new SourceConfig(
            "https://www.coindesk.com/markets/",
            "article.article-card",
            "h4, h3, .headline",
            ".article-body, .content"
        )
    );
    
    @Scheduled(fixedDelayString = "${crawler.fixed-delay:300000}", 
               initialDelayString = "${crawler.initial-delay:60000}")
    public void scheduledCrawl() {
        log.info("Starting scheduled crawl at {}", LocalDateTime.now());
        SOURCES.forEach(this::crawlSource);
        log.info("Completed scheduled crawl at {}", LocalDateTime.now());
    }
    
    public void crawlSource(String sourceName, SourceConfig config) {
        try {
            log.info("Crawling source: {}", sourceName);
            
            Document doc = Jsoup.connect(config.baseUrl())
                    .userAgent("Mozilla/5.0")
                    .timeout(30000)
                    .get();
            
            Elements articles = doc.select(config.articleSelector());
            log.info("Found {} articles from {}", articles.size(), sourceName);
            
            for (Element articleElement : articles) {
                try {
                    publishArticleEvent(articleElement, sourceName, config);
                } catch (Exception e) {
                    log.warn("Failed to process article: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error crawling {}: {}", sourceName, e.getMessage());
        }
    }
    
    private void publishArticleEvent(Element element, String source, SourceConfig config) {
        Element link = element.selectFirst("a[href]");
        if (link == null) return;
        
        String url = link.absUrl("href");
        if (url.isEmpty()) return;
        
        Element titleElement = element.selectFirst(config.titleSelector());
        String title = titleElement != null ? titleElement.text() : "";
        
        ArticleCrawledEvent event = ArticleCrawledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .url(url)
                .title(title)
                .rawHtml(element.outerHtml())
                .source(source)
                .crawledAt(LocalDateTime.now())
                .metadata(new HashMap<>(Map.of("crawler", "phase4")))
                .build();
        
        kafkaTemplate.send(rawArticlesTopic, url, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event: {}", ex.getMessage());
                    } else {
                        log.debug("Published article event: {}", title);
                    }
                });
    }
    
    public record SourceConfig(
        String baseUrl,
        String articleSelector,
        String titleSelector,
        String bodySelector
    ) {}
}
```

---

## Consumer Implementation

### Kafka Consumer Configuration

Create `src/main/java/com/example/backend/config/KafkaConsumerConfig.java`:

```java
package com.example.backend.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }
}
```

### NLP Worker Consumer

Create `src/main/java/com/example/backend/worker/NlpWorker.java`:

```java
package com.example.backend.worker;

import com.example.backend.event.ArticleNormalizedEvent;
import com.example.backend.model.ArticleDocument;
import com.example.backend.nlp.SentimentAnalysisService;
import com.example.backend.repository.ArticleDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NlpWorker {
    
    private final ArticleDocumentRepository articleRepository;
    private final SentimentAnalysisService sentimentService;
    
    @KafkaListener(
        topics = "${kafka.topic.normalized-articles}",
        groupId = "nlp-worker-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processNormalizedArticle(ArticleNormalizedEvent event) {
        log.info("Processing article for NLP: {}", event.getTitle());
        
        try {
            // Find article in database
            articleRepository.findById(event.getArticleId()).ifPresent(article -> {
                // Analyze sentiment
                ArticleDocument.SentimentResult sentiment = 
                    sentimentService.analyzeSentiment(article.getBody());
                article.setSentiment(sentiment);
                
                // Extract symbols
                article.setSymbols(sentimentService.extractSymbols(article.getBody()));
                
                // Extract entities
                article.setEntities(sentimentService.extractEntities(article.getBody()));
                
                articleRepository.save(article);
                log.info("Completed NLP analysis for: {} - Sentiment: {}", 
                    article.getTitle(), sentiment.getLabel());
            });
        } catch (Exception e) {
            log.error("Failed to process NLP for article {}: {}", 
                event.getArticleId(), e.getMessage());
        }
    }
}
```

### Normalizer Worker

Create `src/main/java/com/example/backend/worker/NormalizerWorker.java`:

```java
package com.example.backend.worker;

import com.example.backend.event.ArticleCrawledEvent;
import com.example.backend.event.ArticleNormalizedEvent;
import com.example.backend.model.ArticleDocument;
import com.example.backend.repository.ArticleDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NormalizerWorker {
    
    private final ArticleDocumentRepository articleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.normalized-articles}")
    private String normalizedArticlesTopic;
    
    @KafkaListener(
        topics = "${kafka.topic.raw-articles}",
        groupId = "normalizer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processRawArticle(ArticleCrawledEvent event) {
        log.info("Normalizing article: {}", event.getUrl());
        
        try {
            // Check for duplicate
            if (articleRepository.existsByUrl(event.getUrl())) {
                log.debug("Article already exists: {}", event.getUrl());
                return;
            }
            
            // Parse and normalize
            Document doc = Jsoup.parse(event.getRawHtml());
            String body = doc.text();
            
            // Create and save article
            ArticleDocument article = ArticleDocument.builder()
                    .url(event.getUrl())
                    .title(event.getTitle())
                    .body(body)
                    .source(event.getSource())
                    .rawHtml(event.getRawHtml())
                    .publishedAt(event.getCrawledAt())
                    .crawledAt(event.getCrawledAt())
                    .createdAt(LocalDateTime.now())
                    .metadata(event.getMetadata())
                    .build();
            
            ArticleDocument saved = articleRepository.save(article);
            
            // Publish normalized event for NLP processing
            ArticleNormalizedEvent normalizedEvent = ArticleNormalizedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .articleId(saved.getId())
                    .title(saved.getTitle())
                    .body(saved.getBody())
                    .source(saved.getSource())
                    .publishedAt(saved.getPublishedAt())
                    .build();
            
            kafkaTemplate.send(normalizedArticlesTopic, saved.getUrl(), normalizedEvent);
            log.info("Normalized and published article: {}", event.getTitle());
            
        } catch (Exception e) {
            log.error("Failed to normalize article {}: {}", event.getUrl(), e.getMessage());
        }
    }
}
```

---

## Redis Pub/Sub for WebSocket

### Redis Publisher

Create `src/main/java/com/example/backend/service/PricePublisher.java`:

```java
package com.example.backend.service;

import com.example.backend.event.PriceTickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricePublisher {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void publishPriceTick(PriceTickEvent tick) {
        String channel = "prices:" + tick.getSymbol().toLowerCase();
        redisTemplate.convertAndSend(channel, tick);
        log.debug("Published price tick to {}: {}", channel, tick.getPrice());
    }
}
```

### Redis Subscriber for WebSocket

Create `src/main/java/com/example/backend/config/RedisSubscriberConfig.java`:

```java
package com.example.backend.config;

import com.example.backend.service.WebSocketPriceHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisSubscriberConfig {
    
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("prices:*"));
        return container;
    }
    
    @Bean
    public MessageListenerAdapter listenerAdapter(WebSocketPriceHandler handler) {
        return new MessageListenerAdapter(handler, "handlePriceMessage");
    }
}
```

---

## Load Testing

### k6 Load Test Script

Create `tests/performance/load-test-phase4.js`:

```javascript
import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';

const errorRate = new Rate('errors');
const wsConnections = new Counter('ws_connections');

export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '5m', target: 500 },   // Ramp up to 500 users
    { duration: '5m', target: 1000 },  // Ramp up to 1000 users
    { duration: '5m', target: 1000 },  // Stay at 1000 users
    { duration: '2m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests under 500ms
    http_req_failed: ['rate<0.01'],     // Less than 1% failures
    errors: ['rate<0.05'],              // Less than 5% error rate
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Test Articles API
  const articlesRes = http.get(`${BASE_URL}/api/articles?page=0&size=10`);
  check(articlesRes, {
    'articles status is 200': (r) => r.status === 200,
    'articles response time < 500ms': (r) => r.timings.duration < 500,
  }) || errorRate.add(1);

  // Test Price API
  const priceRes = http.get(`${BASE_URL}/api/prices/historical?symbol=BTCUSDT&interval=1h&limit=100`);
  check(priceRes, {
    'prices status is 200': (r) => r.status === 200,
    'prices response time < 500ms': (r) => r.timings.duration < 500,
  }) || errorRate.add(1);

  sleep(1);
}

export function websocketTest() {
  const url = `ws://${__ENV.WS_HOST || 'localhost:8080'}/ws/prices`;
  
  const res = ws.connect(url, function (socket) {
    wsConnections.add(1);
    
    socket.on('open', () => {
      socket.send(JSON.stringify({
        type: 'subscribe',
        channel: 'btcusdt'
      }));
    });

    socket.on('message', (data) => {
      check(JSON.parse(data), {
        'has price': (m) => m.price !== undefined,
      });
    });

    socket.on('error', (e) => {
      errorRate.add(1);
    });

    socket.setTimeout(function () {
      socket.close();
    }, 60000);
  });

  check(res, { 'ws status is 101': (r) => r && r.status === 101 });
}
```

### Run Load Tests

**Bash (Linux/macOS):**
```bash
# Run API load test
k6 run tests/performance/load-test-phase4.js

# Run with HTML report
k6 run --out json=results.json tests/performance/load-test-phase4.js

# Run WebSocket test
k6 run -e WS_HOST=localhost:8080 tests/performance/load-test-phase4.js
```

**PowerShell (Windows 10/11):**
```powershell
# Run API load test
k6 run tests/performance/load-test-phase4.js

# Run with HTML report
k6 run --out json=results.json tests/performance/load-test-phase4.js

# Run WebSocket test
k6 run -e WS_HOST=localhost:8080 tests/performance/load-test-phase4.js
```

---

## Deployment

### Start All Services

**Bash (Linux/macOS):**
```bash
#!/bin/bash
echo "Starting Phase 4 Infrastructure..."

# Start infrastructure
cd docker
docker compose up -d

# Wait for services
echo "Waiting for services to start..."
sleep 45

# Verify Kafka
echo "Checking Kafka..."
docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092

# Start application
cd ..
./mvnw spring-boot:run -Dspring.profiles.active=dev

echo "Phase 4 deployment complete!"
```

**PowerShell (Windows 10/11):**
```powershell
Write-Host "Starting Phase 4 Infrastructure..." -ForegroundColor Cyan

# Start infrastructure
Set-Location docker
docker compose up -d

# Wait for services
Write-Host "Waiting for services to start..."
Start-Sleep -Seconds 45

# Verify Kafka
Write-Host "Checking Kafka..."
docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092

# Start application
Set-Location ..
.\mvnw.cmd spring-boot:run -D"spring.profiles.active=dev"

Write-Host "Phase 4 deployment complete!" -ForegroundColor Green
```

---

## Common Pitfalls & Troubleshooting

### Issue 1: Kafka Connection Refused

**Symptoms**: `Bootstrap broker localhost:9092 disconnected`

**Solutions**:
1. Wait for Kafka to fully start (30+ seconds)
2. Check if Zookeeper is running
3. Verify KAFKA_ADVERTISED_LISTENERS configuration

**Bash (Linux/macOS):**
```bash
# Check Kafka logs
docker logs trading-kafka

# Check if Kafka is ready
docker exec trading-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

**PowerShell (Windows 10/11):**
```powershell
# Check Kafka logs
docker logs trading-kafka

# Check if Kafka is ready
docker exec trading-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Issue 2: Consumer Not Receiving Messages

**Symptoms**: Messages published but consumer doesn't process them

**Solutions**:
1. Check consumer group ID
2. Verify topic exists
3. Check `auto.offset.reset` configuration
4. Monitor consumer lag:

```bash
docker exec trading-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group trading-platform
```

### Issue 3: Message Serialization Error

**Symptoms**: `SerializationException` or `DeserializationException`

**Solutions**:
1. Ensure trusted packages are configured
2. Verify serializer/deserializer match
3. Add `@JsonTypeInfo` if using polymorphic types

---

## References

### Official Documentation

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [k6 Load Testing](https://k6.io/docs/)

### Related Project Documents

- [Architecture.md](../Architecture.md) - System architecture
- [Phase3-ImplementationGuide.md](./Phase3-ImplementationGuide.md) - Previous phase
- [Phase4-TestingGuide.md](./Phase4-TestingGuide.md) - Testing strategies
