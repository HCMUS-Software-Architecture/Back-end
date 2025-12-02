# Phase 3: Service Separation - Implementation Guide

**Target Scale**: 100-1,000 users  
**Architecture**: Modular Monolith with API Gateway  
**Duration**: Weeks 5-6 (December 28 - January 10, 2026)

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Environment Setup](#environment-setup)
4. [API Gateway Setup](#api-gateway-setup)
5. [Module Boundaries](#module-boundaries)
6. [NLP Module Implementation](#nlp-module-implementation)
7. [NLP Database Schema](#nlp-database-schema)
8. [TradingView Integration](#tradingview-integration)
9. [Frontend Enhancement](#frontend-enhancement)
10. [Deployment](#deployment)
11. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
12. [References](#references)

---

## Overview

### Goals

- Introduce Spring Cloud Gateway as API Gateway
- Define clear service module boundaries
- Implement NLP module with sentiment analysis
- Integrate TradingView charting library
- Link articles to price context
- Implement sentiment-based UI indicators

### Core Requirements Reference

This phase implements requirements from [CoreRequirements.md](../core/CoreRequirements.md):

1. **AI Models for News Analysis** - Sentiment analysis and entity extraction
2. **Causal Analysis** - Trend prediction with reasoning (foundation)
3. **Multiple Currency Pairs** - Symbol-based article filtering

### Database Strategy Reference

From [DatabaseDesign.md](../core/DatabaseDesign.md) - Phase 3 additions:

| Collection | Purpose | New in Phase 3 |
|------------|---------|----------------|
| `analysis_results` | NLP sentiment, entities, topics | ✅ |
| `trend_predictions` | AI-generated predictions | ✅ |

### Architecture Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                         API Gateway                            │
│                    (Spring Cloud Gateway)                      │
└─────────────────────────────┬─────────────────────────────────┘
                              │
    ┌─────────────────────────┼─────────────────────────────┐
    │                         │                             │
┌───┴────┐              ┌─────┴─────┐              ┌────────┴───────┐
│  API   │              │  Crawler  │              │ Price Service  │
│Service │              │  Service  │              │                │
└───┬────┘              └─────┬─────┘              └────────┬───────┘
    │                         │                             │
    └─────────────────────────┼─────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
         PostgreSQL        MongoDB          Redis
```

### Epics for Phase 3

| Epic | Description | Priority |
|------|-------------|----------|
| E3.1 | API Gateway Configuration | High |
| E3.2 | Module Separation | High |
| E3.3 | NLP Sentiment Analysis | High |
| E3.4 | TradingView Integration | High |
| E3.5 | Article-Price Context Linking | Medium |
| E3.6 | Frontend News Feed Enhancement | Medium |

---

## Prerequisites

### Software Requirements

Ensure all Phase 1 and Phase 2 prerequisites are installed, plus:

| Software | Version | Purpose |
|----------|---------|---------|
| Spring Cloud | 2023.0.0+ | Gateway and service discovery |
| Resilience4j | 2.x | Circuit breaker pattern |

---

## Environment Setup

### Step 1: Update Environment Variables

Add to `.env` file:

```bash
# Gateway Configuration
GATEWAY_PORT=8080
API_SERVICE_PORT=8081
CRAWLER_SERVICE_PORT=8082
NLP_SERVICE_PORT=8083

# NLP Configuration
NLP_MODEL_TYPE=keyword  # Options: keyword, huggingface, openai
NLP_BATCH_SIZE=10
NLP_ASYNC_ENABLED=true

# Rate Limiting
RATE_LIMIT_REQUESTS_PER_MINUTE=100
```

### Step 2: Verify Infrastructure

Ensure all Phase 2 services are running:

**Bash (Linux/macOS):**
```bash
cd docker
docker compose up -d

# Verify all services
docker compose ps

# Check service health
curl http://localhost:5432 2>/dev/null || echo "PostgreSQL: OK (connection refused is expected)"
curl http://localhost:27017 2>/dev/null || echo "MongoDB: OK"
docker exec trading-redis redis-cli ping
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location docker
docker compose up -d

# Verify all services
docker compose ps

# Check Redis
docker exec trading-redis redis-cli ping
```

---

## API Gateway Setup

### Step 1: Create Gateway Module

Create a new module structure:

**Bash (Linux/macOS):**
```bash
mkdir -p gateway/src/main/java/com/example/gateway
mkdir -p gateway/src/main/resources
```

**PowerShell (Windows 10/11):**
```powershell
New-Item -ItemType Directory -Force -Path gateway/src/main/java/com/example/gateway
New-Item -ItemType Directory -Force -Path gateway/src/main/resources
```

### Step 2: Gateway Dependencies

Create `gateway/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>trading-gateway</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Trading Platform Gateway</name>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Step 3: Gateway Configuration

Create `gateway/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: trading-gateway
  cloud:
    gateway:
      routes:
        # Article Service Routes
        - id: articles-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/articles/**
          filters:
            - RewritePath=/api/articles/(?<segment>.*), /api/articles/${segment}
            - AddRequestHeader=X-Gateway-Source, trading-gateway
            
        # Price Service Routes
        - id: price-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/prices/**
          filters:
            - RewritePath=/api/prices/(?<segment>.*), /api/prices/${segment}
            
        # NLP Service Routes
        - id: nlp-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/analysis/**
          filters:
            - RewritePath=/api/analysis/(?<segment>.*), /api/analysis/${segment}
            
        # WebSocket Routes
        - id: websocket-route
          uri: ws://localhost:8082
          predicates:
            - Path=/ws/**
            
      # Global Filters
      default-filters:
        - AddResponseHeader=X-Response-Time, ${date.time}
        
      # CORS Configuration
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            
# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,gateway

# Resilience4j Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
```

### Step 4: Gateway Application

Create `gateway/src/main/java/com/example/gateway/GatewayApplication.java`:

```java
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

### Step 5: Rate Limiting Filter

Create `gateway/src/main/java/com/example/gateway/filter/RateLimitFilter.java`:

```java
package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {
    
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    
    public RateLimitFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            
            AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
            
            if (count.incrementAndGet() > config.getMaxRequests()) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            
            // Reset count after window (simplified - use Redis in production)
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // Cleanup logic here
            }));
        };
    }
    
    public static class Config {
        private int maxRequests = 100;
        
        public int getMaxRequests() {
            return maxRequests;
        }
        
        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }
    }
}
```

---

## Module Boundaries

### Service Module Structure

```
back-end/
├── gateway/                    # API Gateway
│   └── src/main/java/com/example/gateway/
├── modules/
│   ├── api-service/            # REST API Module
│   │   └── src/main/java/com/example/api/
│   ├── crawler-service/        # Crawler Module
│   │   └── src/main/java/com/example/crawler/
│   ├── price-service/          # Price Collection Module
│   │   └── src/main/java/com/example/price/
│   └── nlp-service/            # NLP Analysis Module
│       └── src/main/java/com/example/nlp/
└── common/                     # Shared Models & Utils
    └── src/main/java/com/example/common/
```

### Create Module Directories

**Bash (Linux/macOS):**
```bash
mkdir -p modules/api-service/src/main/java/com/example/api
mkdir -p modules/crawler-service/src/main/java/com/example/crawler
mkdir -p modules/price-service/src/main/java/com/example/price
mkdir -p modules/nlp-service/src/main/java/com/example/nlp
mkdir -p common/src/main/java/com/example/common
```

**PowerShell (Windows 10/11):**
```powershell
New-Item -ItemType Directory -Force -Path modules/api-service/src/main/java/com/example/api
New-Item -ItemType Directory -Force -Path modules/crawler-service/src/main/java/com/example/crawler
New-Item -ItemType Directory -Force -Path modules/price-service/src/main/java/com/example/price
New-Item -ItemType Directory -Force -Path modules/nlp-service/src/main/java/com/example/nlp
New-Item -ItemType Directory -Force -Path common/src/main/java/com/example/common
```

---

## NLP Module Implementation

### Step 1: NLP Dependencies

Add to main `pom.xml`:

```xml
<!-- NLP Dependencies -->
<dependency>
    <groupId>edu.stanford.nlp</groupId>
    <artifactId>stanford-corenlp</artifactId>
    <version>4.5.5</version>
</dependency>

<!-- OR use external API client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### Step 2: Sentiment Analysis Service

Create `src/main/java/com/example/backend/nlp/SentimentAnalysisService.java`:

```java
package com.example.backend.nlp;

import com.example.backend.model.ArticleDocument;
import com.example.backend.repository.ArticleDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisService {
    
    private final ArticleDocumentRepository articleRepository;
    private final WebClient.Builder webClientBuilder;
    
    @Value("${nlp.api.url:https://api.example.com/sentiment}")
    private String nlpApiUrl;
    
    // Simple keyword-based sentiment (fallback)
    private static final Map<String, Double> SENTIMENT_KEYWORDS = Map.ofEntries(
        Map.entry("bullish", 1.0),
        Map.entry("surge", 0.8),
        Map.entry("rally", 0.7),
        Map.entry("gains", 0.6),
        Map.entry("positive", 0.5),
        Map.entry("bearish", -1.0),
        Map.entry("crash", -0.9),
        Map.entry("plunge", -0.8),
        Map.entry("losses", -0.6),
        Map.entry("negative", -0.5)
    );
    
    // Crypto symbol patterns
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
        "\\b(BTC|ETH|BNB|XRP|SOL|ADA|DOGE|DOT|MATIC|LINK|USDT|USDC)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    @Async
    public void analyzeArticle(String articleId) {
        articleRepository.findById(articleId).ifPresent(article -> {
            try {
                // Extract symbols
                List<String> symbols = extractSymbols(article.getBody());
                article.setSymbols(symbols);
                
                // Analyze sentiment
                ArticleDocument.SentimentResult sentiment = analyzeSentiment(article.getBody());
                article.setSentiment(sentiment);
                
                // Extract entities (simplified)
                List<String> entities = extractEntities(article.getBody());
                article.setEntities(entities);
                
                articleRepository.save(article);
                log.info("Analyzed article: {} - Sentiment: {}", article.getTitle(), sentiment.getLabel());
                
            } catch (Exception e) {
                log.error("Failed to analyze article {}: {}", articleId, e.getMessage());
            }
        });
    }
    
    public ArticleDocument.SentimentResult analyzeSentiment(String text) {
        if (text == null || text.isEmpty()) {
            return createNeutralSentiment();
        }
        
        String lowerText = text.toLowerCase();
        double score = 0.0;
        int matchCount = 0;
        
        for (Map.Entry<String, Double> entry : SENTIMENT_KEYWORDS.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                score += entry.getValue();
                matchCount++;
            }
        }
        
        if (matchCount > 0) {
            score = score / matchCount;
        }
        
        String label;
        if (score > 0.2) {
            label = "bullish";
        } else if (score < -0.2) {
            label = "bearish";
        } else {
            label = "neutral";
        }
        
        return ArticleDocument.SentimentResult.builder()
                .label(label)
                .score(Math.abs(score))
                .model("keyword-based")
                .build();
    }
    
    public List<String> extractSymbols(String text) {
        if (text == null) return List.of();
        
        Matcher matcher = SYMBOL_PATTERN.matcher(text);
        return matcher.results()
                .map(m -> m.group().toUpperCase())
                .distinct()
                .toList();
    }
    
    public List<String> extractEntities(String text) {
        // Simplified entity extraction - in production use NER
        Pattern entityPattern = Pattern.compile(
            "\\b(Binance|Coinbase|Bitcoin|Ethereum|SEC|Fed|Federal Reserve)\\b",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = entityPattern.matcher(text);
        return matcher.results()
                .map(m -> m.group())
                .distinct()
                .toList();
    }
    
    private ArticleDocument.SentimentResult createNeutralSentiment() {
        return ArticleDocument.SentimentResult.builder()
                .label("neutral")
                .score(0.0)
                .model("keyword-based")
                .build();
    }
}
```

### Step 3: NLP Controller

Create `src/main/java/com/example/backend/controller/AnalysisController.java`:

```java
package com.example.backend.controller;

import com.example.backend.model.ArticleDocument;
import com.example.backend.nlp.SentimentAnalysisService;
import com.example.backend.repository.ArticleDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    
    private final SentimentAnalysisService sentimentService;
    private final ArticleDocumentRepository articleRepository;
    
    @GetMapping("/{articleId}")
    public ResponseEntity<Map<String, Object>> getAnalysis(@PathVariable String articleId) {
        return articleRepository.findById(articleId)
                .map(article -> ResponseEntity.ok(Map.of(
                    "articleId", article.getId(),
                    "title", article.getTitle(),
                    "sentiment", article.getSentiment(),
                    "symbols", article.getSymbols(),
                    "entities", article.getEntities()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/analyze/{articleId}")
    public ResponseEntity<Map<String, String>> triggerAnalysis(@PathVariable String articleId) {
        sentimentService.analyzeArticle(articleId);
        return ResponseEntity.accepted().body(Map.of(
            "status", "processing",
            "articleId", articleId
        ));
    }
    
    @GetMapping("/sentiment/{label}")
    public ResponseEntity<Page<ArticleDocument>> getArticlesBySentiment(
            @PathVariable String label,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ArticleDocument> articles = articleRepository.findBySentiment(
            label, 
            PageRequest.of(page, size)
        );
        return ResponseEntity.ok(articles);
    }
    
    @GetMapping("/symbols/{symbol}")
    public ResponseEntity<Page<ArticleDocument>> getArticlesBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // This would need a custom query in the repository
        return ResponseEntity.ok(Page.empty());
    }
}
```

---

## NLP Database Schema

This section implements the NLP-related database schema from [DatabaseDesign.md](../core/DatabaseDesign.md).

### MongoDB Collection: analysis_results

Create the MongoDB schema for NLP results:

```javascript
// MongoDB collection for NLP analysis results
// Reference: DatabaseDesign.md - Collection: analysis_results

db.createCollection('analysis_results');

// Create indexes
db.analysis_results.createIndex({ "article_id": 1 }, { unique: true });
db.analysis_results.createIndex({ "sentiment.label": 1, "analyzed_at": -1 });
db.analysis_results.createIndex({ "entities.symbol": 1 });
db.analysis_results.createIndex({ "trend_prediction.direction": 1 });
db.analysis_results.createIndex({ "analyzed_at": -1 });
```

### Analysis Result Document Structure

```java
package com.example.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String articleId;
    
    private String modelVersion;
    private LocalDateTime analyzedAt;
    private Long processingTimeMs;
    
    // Sentiment analysis
    private SentimentData sentiment;
    
    // Entity extraction
    private List<Entity> entities;
    
    // Topic classification
    private List<Topic> topics;
    
    // Key phrases
    private List<String> keyPhrases;
    
    // Causal signals (for advanced analysis)
    private List<CausalSignal> causalSignals;
    
    // Trend prediction
    private TrendPrediction trendPrediction;
    
    // Quality metrics
    private QualityMetrics qualityMetrics;
    
    @Data
    @Builder
    public static class SentimentData {
        private String label;        // BULLISH, BEARISH, NEUTRAL
        private double score;
        private double confidence;
        private Distribution distribution;
        
        @Data
        @Builder
        public static class Distribution {
            private double bullish;
            private double bearish;
            private double neutral;
        }
    }
    
    @Data
    @Builder
    public static class Entity {
        private String text;
        private String type;         // CRYPTOCURRENCY, ORGANIZATION, PERSON
        private String symbol;       // e.g., BTCUSDT
        private int count;
        private double relevance;
    }
    
    @Data
    @Builder
    public static class Topic {
        private String name;
        private double score;
        private List<String> keywords;
    }
    
    @Data
    @Builder
    public static class CausalSignal {
        private String cause;
        private String effect;
        private double confidence;
        private String timeHorizon;
    }
    
    @Data
    @Builder
    public static class TrendPrediction {
        private String direction;    // UP, DOWN, NEUTRAL
        private double confidence;
        private String timeHorizon;  // 1h, 24h, 7d
        private String reasoning;
    }
    
    @Data
    @Builder
    public static class QualityMetrics {
        private double textQuality;
        private double informationDensity;
        private double actionability;
    }
}
```

### MongoDB Collection: trend_predictions

```java
package com.example.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "trend_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendPrediction {
    
    @Id
    private String id;
    
    @Indexed
    private String symbol;
    
    @Indexed
    private LocalDateTime predictionTime;
    
    private String timeHorizon;     // 1h, 24h, 7d
    
    private PredictionData prediction;
    
    private List<ContributingArticle> contributingArticles;
    
    private SentimentSummary sentimentSummary;
    
    private String reasoning;
    
    private ModelMetadata modelMetadata;
    
    // Filled after time_horizon passes
    private String actualOutcome;
    private Double accuracyScore;
    
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    public static class PredictionData {
        private String direction;    // UP, DOWN, NEUTRAL
        private double confidence;
        private PriceTarget priceTarget;
        
        @Data
        @Builder
        public static class PriceTarget {
            private double low;
            private double mid;
            private double high;
        }
    }
    
    @Data
    @Builder
    public static class ContributingArticle {
        private String articleId;
        private String title;
        private double weight;
    }
    
    @Data
    @Builder
    public static class SentimentSummary {
        private int bullishCount;
        private int bearishCount;
        private int neutralCount;
        private double averageScore;
    }
    
    @Data
    @Builder
    public static class ModelMetadata {
        private String version;
        private List<String> featuresUsed;
    }
}
```

### Create MongoDB Indexes

Add to `docker/mongo-init.js`:

```javascript
// Phase 3: NLP Analysis collections

// Create analysis_results collection with indexes
db.createCollection('analysis_results');
db.analysis_results.createIndex({ "article_id": 1 }, { unique: true });
db.analysis_results.createIndex({ "sentiment.label": 1, "analyzed_at": -1 });
db.analysis_results.createIndex({ "entities.symbol": 1 });
db.analysis_results.createIndex({ "trend_prediction.direction": 1 });
db.analysis_results.createIndex({ "analyzed_at": -1 });

// Create trend_predictions collection with indexes
db.createCollection('trend_predictions');
db.trend_predictions.createIndex({ "symbol": 1, "prediction_time": -1 });
db.trend_predictions.createIndex({ "prediction_time": -1 });
db.trend_predictions.createIndex({ "prediction.direction": 1, "prediction.confidence": -1 });

print("Phase 3 NLP collections initialized!");
```

---

## TradingView Integration

### Step 1: TradingView Library Setup

For the frontend, install TradingView's lightweight charts:

**Bash (Linux/macOS):**
```bash
cd frontend
npm install lightweight-charts
npm install @tanstack/react-query axios
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location frontend
npm install lightweight-charts
npm install @tanstack/react-query axios
```

### Step 2: Price Chart Component

Create `frontend/src/components/PriceChart.tsx`:

```typescript
import React, { useEffect, useRef, useState } from 'react';
import { createChart, IChartApi, ISeriesApi, CandlestickData } from 'lightweight-charts';

interface PriceChartProps {
  symbol: string;
  interval: string;
}

export const PriceChart: React.FC<PriceChartProps> = ({ symbol, interval }) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  
  useEffect(() => {
    if (!chartContainerRef.current) return;
    
    // Create chart
    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: 400,
      layout: {
        background: { color: '#1a1a2e' },
        textColor: '#d1d4dc',
      },
      grid: {
        vertLines: { color: '#2B2B43' },
        horzLines: { color: '#2B2B43' },
      },
      crosshair: {
        mode: 0,
      },
      rightPriceScale: {
        borderColor: '#2B2B43',
      },
      timeScale: {
        borderColor: '#2B2B43',
        timeVisible: true,
      },
    });
    
    chartRef.current = chart;
    
    // Add candlestick series
    const candlestickSeries = chart.addCandlestickSeries({
      upColor: '#26a69a',
      downColor: '#ef5350',
      borderVisible: false,
      wickUpColor: '#26a69a',
      wickDownColor: '#ef5350',
    });
    
    seriesRef.current = candlestickSeries;
    
    // Fetch historical data
    fetchHistoricalData(symbol, interval).then(data => {
      candlestickSeries.setData(data);
    });
    
    // Connect to WebSocket for real-time updates
    const ws = new WebSocket(`ws://localhost:8080/ws/prices/${symbol.toLowerCase()}`);
    
    ws.onmessage = (event) => {
      const tick = JSON.parse(event.data);
      candlestickSeries.update({
        time: Math.floor(tick.timestamp / 1000) as any,
        open: parseFloat(tick.price),
        high: parseFloat(tick.price),
        low: parseFloat(tick.price),
        close: parseFloat(tick.price),
      });
    };
    
    // Handle resize
    const handleResize = () => {
      if (chartContainerRef.current) {
        chart.applyOptions({ width: chartContainerRef.current.clientWidth });
      }
    };
    
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('resize', handleResize);
      ws.close();
      chart.remove();
    };
  }, [symbol, interval]);
  
  return (
    <div className="price-chart-container">
      <div className="chart-header">
        <h2>{symbol} - {interval}</h2>
      </div>
      <div ref={chartContainerRef} className="chart" />
    </div>
  );
};

async function fetchHistoricalData(symbol: string, interval: string): Promise<CandlestickData[]> {
  const response = await fetch(
    `http://localhost:8080/api/prices/historical?symbol=${symbol}&interval=${interval}&limit=100`
  );
  const candles = await response.json();
  
  return candles.map((candle: any) => ({
    time: Math.floor(new Date(candle.openTime).getTime() / 1000),
    open: parseFloat(candle.open),
    high: parseFloat(candle.high),
    low: parseFloat(candle.low),
    close: parseFloat(candle.close),
  }));
}
```

### Step 3: Symbol Selector Component

Create `frontend/src/components/SymbolSelector.tsx`:

```typescript
import React from 'react';

interface SymbolSelectorProps {
  selectedSymbol: string;
  onSymbolChange: (symbol: string) => void;
}

const AVAILABLE_SYMBOLS = ['BTCUSDT', 'ETHUSDT', 'BNBUSDT', 'SOLUSDT'];
const INTERVALS = ['1m', '5m', '15m', '1h', '4h', '1d'];

export const SymbolSelector: React.FC<SymbolSelectorProps> = ({
  selectedSymbol,
  onSymbolChange,
}) => {
  return (
    <div className="symbol-selector">
      <select 
        value={selectedSymbol} 
        onChange={(e) => onSymbolChange(e.target.value)}
        className="symbol-dropdown"
      >
        {AVAILABLE_SYMBOLS.map(symbol => (
          <option key={symbol} value={symbol}>{symbol}</option>
        ))}
      </select>
    </div>
  );
};
```

---

## Frontend Enhancement

### News Feed with Sentiment

Create `frontend/src/components/NewsFeed.tsx`:

```typescript
import React from 'react';
import { useQuery } from '@tanstack/react-query';

interface Article {
  id: string;
  title: string;
  source: string;
  publishedAt: string;
  sentiment?: {
    label: string;
    score: number;
  };
  symbols?: string[];
}

export const NewsFeed: React.FC<{ symbol?: string }> = ({ symbol }) => {
  const { data: articles, isLoading } = useQuery({
    queryKey: ['articles', symbol],
    queryFn: () => fetchArticles(symbol),
    refetchInterval: 60000, // Refresh every minute
  });
  
  if (isLoading) return <div className="loading">Loading articles...</div>;
  
  return (
    <div className="news-feed">
      <h2>Latest News</h2>
      {articles?.map((article: Article) => (
        <ArticleCard key={article.id} article={article} />
      ))}
    </div>
  );
};

const ArticleCard: React.FC<{ article: Article }> = ({ article }) => {
  const sentimentColor = getSentimentColor(article.sentiment?.label);
  
  return (
    <div className="article-card">
      <div className="article-header">
        <span className="source">{article.source}</span>
        <span 
          className="sentiment-badge"
          style={{ backgroundColor: sentimentColor }}
        >
          {article.sentiment?.label || 'N/A'}
        </span>
      </div>
      <h3 className="article-title">{article.title}</h3>
      <div className="article-meta">
        <span className="date">
          {new Date(article.publishedAt).toLocaleDateString()}
        </span>
        <div className="symbols">
          {article.symbols?.map(s => (
            <span key={s} className="symbol-tag">{s}</span>
          ))}
        </div>
      </div>
    </div>
  );
};

function getSentimentColor(sentiment?: string): string {
  switch (sentiment) {
    case 'bullish': return '#26a69a';
    case 'bearish': return '#ef5350';
    default: return '#888';
  }
}

async function fetchArticles(symbol?: string): Promise<Article[]> {
  const url = symbol 
    ? `http://localhost:8080/api/articles/symbol/${symbol}`
    : 'http://localhost:8080/api/articles';
  const response = await fetch(url);
  const data = await response.json();
  return data.content || [];
}
```

---

## Deployment

### Multi-Service Startup

**Bash (Linux/macOS):**
```bash
#!/bin/bash
# Start infrastructure
cd docker
docker compose up -d

# Wait for services
sleep 15

# Start Gateway
cd ../gateway
./mvnw spring-boot:run -Dserver.port=8080 &

# Start API Service
cd ../
./mvnw spring-boot:run -Dserver.port=8081 &

# Wait for all services
echo "Services starting..."
sleep 30

# Verify
curl http://localhost:8080/api/health
```

**PowerShell (Windows 10/11):**
```powershell
# Start infrastructure
Set-Location docker
docker compose up -d

# Wait for services
Start-Sleep -Seconds 15

# Start Gateway (in new process)
Start-Process -FilePath "cmd" -ArgumentList "/c cd ..\gateway && mvnw.cmd spring-boot:run -Dserver.port=8080"

# Start API Service (in new process)
Start-Process -FilePath "cmd" -ArgumentList "/c mvnw.cmd spring-boot:run -Dserver.port=8081"

# Wait for all services
Write-Host "Services starting..."
Start-Sleep -Seconds 30

# Verify
Invoke-RestMethod -Uri http://localhost:8080/api/health
```

---

## Common Pitfalls & Troubleshooting

### Issue 1: Gateway Route Not Found

**Symptoms**: `404 Not Found` when accessing through gateway

**Solutions**:
1. Check route predicates in `application.yml`
2. Verify backend service is running on expected port
3. Check gateway logs for routing decisions

### Issue 2: CORS Errors

**Symptoms**: `CORS policy blocked` in browser

**Solution**: Ensure CORS is configured in gateway:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
```

### Issue 3: WebSocket Connection Failed

**Symptoms**: `WebSocket is closed before the connection is established`

**Solutions**:
1. Check WebSocket route in gateway
2. Ensure `ws://` protocol is used
3. Verify SockJS fallback is configured

### Issue 4: NLP Analysis Slow

**Symptoms**: Sentiment analysis takes too long

**Solutions**:
1. Use async processing (`@Async`)
2. Consider batch processing
3. Use simpler keyword-based approach for real-time

---

## References

### Official Documentation

- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Resilience4j](https://resilience4j.readme.io/docs)
- [TradingView Lightweight Charts](https://tradingview.github.io/lightweight-charts/)
- [Stanford CoreNLP](https://stanfordnlp.github.io/CoreNLP/)

### Related Project Documents

- [CoreRequirements.md](../core/CoreRequirements.md) - Business requirements
- [DatabaseDesign.md](../core/DatabaseDesign.md) - Database architecture (NLP collections)
- [UIUXGuidelines.md](../core/UIUXGuidelines.md) - UI/UX design guidelines
- [Phase2-ImplementationGuide.md](./Phase2-ImplementationGuide.md) - Previous phase
- [Phase4-ImplementationGuide.md](./Phase4-ImplementationGuide.md) - Next phase
