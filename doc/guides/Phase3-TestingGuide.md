# Phase 3: Service Separation - Testing & Verification Guide

**Target Scale**: 100-1,000 users  
**Phase Duration**: Weeks 5-6 (December 28 - January 10, 2026)

---

## Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [API Gateway Testing](#api-gateway-testing)
3. [NLP Module Testing](#nlp-module-testing)
4. [Integration Testing](#integration-testing)
5. [Frontend Testing](#frontend-testing)
6. [End-to-End Testing](#end-to-end-testing)
7. [Manual Testing Checklist](#manual-testing-checklist)
8. [Acceptance Criteria Verification](#acceptance-criteria-verification)
9. [References](#references)

---

## Testing Strategy Overview

### Testing Focus for Phase 3

| Component | Test Types | Priority |
|-----------|------------|----------|
| API Gateway | Routing, Filters, Circuit Breaker | High |
| NLP Service | Sentiment Analysis, Entity Extraction | High |
| TradingView Integration | Chart Rendering, Data Binding | Medium |
| Module Communication | Inter-module API calls | High |

### Additional Testing Tools

| Tool | Purpose | Version |
|------|---------|---------|
| WireMock | Mock external APIs | 3.x |
| Cypress | E2E frontend testing | 13.x |
| Jest | React component testing | 29.x |
| Playwright | Cross-browser testing | Latest |

---

## API Gateway Testing

### Gateway Route Tests

Create `gateway/src/test/java/com/example/gateway/GatewayRoutingTest.java`:

```java
package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
class GatewayRoutingTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldRouteToArticlesService() {
        // Given
        stubFor(get(urlPathMatching("/api/articles"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"content\": []}")
                .withStatus(200)));
        
        // When & Then
        webTestClient.get()
            .uri("/api/articles")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content").isArray();
    }
    
    @Test
    void shouldRouteToPriceService() {
        // Given
        stubFor(get(urlPathMatching("/api/prices/current/BTCUSDT"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"symbol\": \"BTCUSDT\", \"price\": 50000}")
                .withStatus(200)));
        
        // When & Then
        webTestClient.get()
            .uri("/api/prices/current/BTCUSDT")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.symbol").isEqualTo("BTCUSDT");
    }
    
    @Test
    void shouldAddGatewayHeader() {
        stubFor(get(urlPathMatching("/api/articles"))
            .willReturn(aResponse().withStatus(200)));
        
        webTestClient.get()
            .uri("/api/articles")
            .exchange()
            .expectHeader().exists("X-Response-Time");
    }
    
    @Test
    void shouldReturn404ForUnknownRoute() {
        webTestClient.get()
            .uri("/api/unknown")
            .exchange()
            .expectStatus().isNotFound();
    }
}
```

### Circuit Breaker Tests

Create `gateway/src/test/java/com/example/gateway/CircuitBreakerTest.java`:

```java
package com.example.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CircuitBreakerTest {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Test
    void shouldHaveCircuitBreakerConfigured() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("default");
        
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
    }
    
    @Test
    void shouldOpenCircuitAfterFailures() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("default");
        
        // Simulate failures
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("Test failure"));
        }
        
        // Circuit should be open or half-open
        assertThat(circuitBreaker.getState()).isIn(
            CircuitBreaker.State.OPEN, 
            CircuitBreaker.State.HALF_OPEN
        );
    }
}
```

---

## NLP Module Testing

### Sentiment Analysis Tests

Create `src/test/java/com/example/backend/nlp/SentimentAnalysisServiceTest.java`:

```java
package com.example.backend.nlp;

import com.example.backend.model.ArticleDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SentimentAnalysisServiceTest {
    
    @InjectMocks
    private SentimentAnalysisService sentimentService;
    
    @Test
    void shouldDetectBullishSentiment() {
        String text = "Bitcoin shows strong bullish momentum with significant gains expected";
        
        ArticleDocument.SentimentResult result = sentimentService.analyzeSentiment(text);
        
        assertThat(result.getLabel()).isEqualTo("bullish");
        assertThat(result.getScore()).isGreaterThan(0);
    }
    
    @Test
    void shouldDetectBearishSentiment() {
        String text = "Market crash imminent as bearish signals dominate, losses expected";
        
        ArticleDocument.SentimentResult result = sentimentService.analyzeSentiment(text);
        
        assertThat(result.getLabel()).isEqualTo("bearish");
        assertThat(result.getScore()).isGreaterThan(0);
    }
    
    @Test
    void shouldDetectNeutralSentiment() {
        String text = "Bitcoin price remains stable today with mixed signals";
        
        ArticleDocument.SentimentResult result = sentimentService.analyzeSentiment(text);
        
        assertThat(result.getLabel()).isEqualTo("neutral");
    }
    
    @Test
    void shouldExtractSymbols() {
        String text = "BTC and ETH are leading the market. DOGE also showing strength.";
        
        List<String> symbols = sentimentService.extractSymbols(text);
        
        assertThat(symbols).containsExactlyInAnyOrder("BTC", "ETH", "DOGE");
    }
    
    @Test
    void shouldExtractSymbolsCaseInsensitive() {
        String text = "btc price increases while eth remains stable";
        
        List<String> symbols = sentimentService.extractSymbols(text);
        
        assertThat(symbols).containsExactlyInAnyOrder("BTC", "ETH");
    }
    
    @Test
    void shouldExtractEntities() {
        String text = "Binance announces new partnership with SEC approval pending";
        
        List<String> entities = sentimentService.extractEntities(text);
        
        assertThat(entities).contains("Binance", "SEC");
    }
    
    @Test
    void shouldHandleEmptyText() {
        ArticleDocument.SentimentResult result = sentimentService.analyzeSentiment("");
        
        assertThat(result.getLabel()).isEqualTo("neutral");
        assertThat(result.getScore()).isEqualTo(0.0);
    }
    
    @Test
    void shouldHandleNullText() {
        ArticleDocument.SentimentResult result = sentimentService.analyzeSentiment(null);
        
        assertThat(result.getLabel()).isEqualTo("neutral");
    }
}
```

### Analysis Controller Tests

Create `src/test/java/com/example/backend/controller/AnalysisControllerTest.java`:

```java
package com.example.backend.controller;

import com.example.backend.model.ArticleDocument;
import com.example.backend.nlp.SentimentAnalysisService;
import com.example.backend.repository.ArticleDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private SentimentAnalysisService sentimentService;
    
    @MockBean
    private ArticleDocumentRepository articleRepository;
    
    @Test
    void shouldReturnArticleAnalysis() throws Exception {
        // Given
        ArticleDocument article = ArticleDocument.builder()
                .id("test-id")
                .title("Test Article")
                .sentiment(ArticleDocument.SentimentResult.builder()
                    .label("bullish")
                    .score(0.8)
                    .build())
                .symbols(List.of("BTC", "ETH"))
                .entities(List.of("Binance"))
                .build();
        
        when(articleRepository.findById("test-id")).thenReturn(Optional.of(article));
        
        // When & Then
        mockMvc.perform(get("/api/analysis/test-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value("test-id"))
                .andExpect(jsonPath("$.sentiment.label").value("bullish"))
                .andExpect(jsonPath("$.symbols").isArray());
    }
    
    @Test
    void shouldReturn404ForNonExistentArticle() throws Exception {
        when(articleRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/analysis/non-existent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldTriggerAnalysis() throws Exception {
        mockMvc.perform(post("/api/analysis/analyze/test-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("processing"));
        
        verify(sentimentService).analyzeArticle("test-id");
    }
}
```

---

## Integration Testing

### Full Stack Integration Test

Create `src/test/java/com/example/backend/integration/Phase3IntegrationTest.java`:

```java
package com.example.backend.integration;

import com.example.backend.model.ArticleDocument;
import com.example.backend.nlp.SentimentAnalysisService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class Phase3IntegrationTest {
    
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
    
    @Autowired
    private SentimentAnalysisService sentimentService;
    
    private ArticleDocument testArticle;
    
    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        
        testArticle = ArticleDocument.builder()
                .url("https://example.com/bullish-btc")
                .title("Bitcoin shows bullish momentum with strong gains")
                .body("BTC price rally continues with bullish indicators. ETH also showing positive momentum.")
                .source("coindesk")
                .publishedAt(LocalDateTime.now())
                .build();
        
        testArticle = articleRepository.save(testArticle);
    }
    
    @Test
    void shouldAnalyzeArticleAndReturnSentiment() throws Exception {
        // Trigger analysis
        mockMvc.perform(post("/api/analysis/analyze/" + testArticle.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
        
        // Wait for async processing
        Thread.sleep(1000);
        
        // Check analysis result
        mockMvc.perform(get("/api/analysis/" + testArticle.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentiment.label").value("bullish"))
                .andExpect(jsonPath("$.symbols", containsInAnyOrder("BTC", "ETH")));
    }
    
    @Test
    void shouldFilterBySentiment() throws Exception {
        // First analyze the article
        sentimentService.analyzeArticle(testArticle.getId());
        Thread.sleep(500);
        
        // Query by sentiment
        mockMvc.perform(get("/api/analysis/sentiment/bullish")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
```

---

## Frontend Testing

### Jest Configuration

Create `frontend/jest.config.js`:

```javascript
module.exports = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
  },
  testPathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/.next/'],
  transform: {
    '^.+\\.(js|jsx|ts|tsx)$': ['babel-jest', { presets: ['next/babel'] }],
  },
};
```

### Component Tests

Create `frontend/src/components/__tests__/NewsFeed.test.tsx`:

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { NewsFeed } from '../NewsFeed';

// Mock fetch
global.fetch = jest.fn();

const mockArticles = {
  content: [
    {
      id: '1',
      title: 'Bitcoin reaches new high',
      source: 'coindesk',
      publishedAt: '2025-01-01T10:00:00',
      sentiment: { label: 'bullish', score: 0.8 },
      symbols: ['BTC'],
    },
    {
      id: '2',
      title: 'Ethereum update',
      source: 'cointelegraph',
      publishedAt: '2025-01-01T09:00:00',
      sentiment: { label: 'neutral', score: 0.5 },
      symbols: ['ETH'],
    },
  ],
};

describe('NewsFeed', () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  beforeEach(() => {
    (fetch as jest.Mock).mockResolvedValue({
      json: () => Promise.resolve(mockArticles),
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders articles', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <NewsFeed />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Bitcoin reaches new high')).toBeInTheDocument();
      expect(screen.getByText('Ethereum update')).toBeInTheDocument();
    });
  });

  it('displays sentiment badges', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <NewsFeed />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('bullish')).toBeInTheDocument();
      expect(screen.getByText('neutral')).toBeInTheDocument();
    });
  });

  it('displays symbol tags', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <NewsFeed />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('BTC')).toBeInTheDocument();
      expect(screen.getByText('ETH')).toBeInTheDocument();
    });
  });
});
```

### Run Frontend Tests

**Bash (Linux/macOS):**
```bash
cd frontend
npm test
npm run test:coverage
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location frontend
npm test
npm run test:coverage
```

---

## End-to-End Testing

### Cypress E2E Tests

Create `frontend/cypress/e2e/dashboard.cy.ts`:

```typescript
describe('Dashboard', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/articles*', { fixture: 'articles.json' });
    cy.intercept('GET', '/api/prices/historical*', { fixture: 'candles.json' });
    cy.visit('/');
  });

  it('displays the price chart', () => {
    cy.get('.price-chart-container').should('exist');
    cy.get('.chart').should('be.visible');
  });

  it('displays news feed', () => {
    cy.get('.news-feed').should('exist');
    cy.get('.article-card').should('have.length.at.least', 1);
  });

  it('shows sentiment indicators', () => {
    cy.get('.sentiment-badge').should('exist');
  });

  it('allows symbol selection', () => {
    cy.get('.symbol-dropdown').select('ETHUSDT');
    cy.get('.chart-header h2').should('contain', 'ETHUSDT');
  });

  it('clicking article shows details', () => {
    cy.get('.article-card').first().click();
    cy.get('.article-detail').should('be.visible');
  });
});
```

### Run E2E Tests

**Bash (Linux/macOS):**
```bash
cd frontend

# Run Cypress tests headlessly
npm run cypress:run

# Open Cypress UI
npm run cypress:open
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location frontend

# Run Cypress tests headlessly
npm run cypress:run

# Open Cypress UI
npm run cypress:open
```

---

## Manual Testing Checklist

### API Gateway

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-301 | Access `/api/articles` via gateway | Routes to articles service | ☐ |
| MT-302 | Access `/api/prices/current/BTCUSDT` | Routes to price service | ☐ |
| MT-303 | Check response headers | Contains `X-Response-Time` | ☐ |
| MT-304 | Test rate limiting | Returns 429 after limit | ☐ |

### NLP Module

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-310 | Analyze bullish article | Returns bullish sentiment | ☐ |
| MT-311 | Analyze bearish article | Returns bearish sentiment | ☐ |
| MT-312 | Extract symbols | Correctly identifies crypto symbols | ☐ |
| MT-313 | Trigger async analysis | Returns 202 Accepted | ☐ |

### TradingView Chart

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-320 | Load chart page | Chart renders with candles | ☐ |
| MT-321 | Change symbol | Chart updates with new data | ☐ |
| MT-322 | Change interval | Candles adjust to timeframe | ☐ |
| MT-323 | Real-time update | New ticks appear on chart | ☐ |

### News Feed

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-330 | Load news feed | Articles displayed with sentiment | ☐ |
| MT-331 | Filter by sentiment | Only matching articles shown | ☐ |
| MT-332 | Click article | Shows detail with symbols | ☐ |
| MT-333 | Refresh feed | New articles appear | ☐ |

---

## Acceptance Criteria Verification

### Phase 3 Acceptance Criteria

| Criteria | Test Method | Command | Status |
|----------|-------------|---------|--------|
| Gateway routing all requests | API test | `curl http://localhost:8080/api/articles` | ☐ |
| Sentiment analysis on articles | Check article | Query `/api/analysis/{id}` | ☐ |
| TradingView chart functional | Browser test | Navigate to chart page | ☐ |
| Module separation | Code review | Check package structure | ☐ |
| Article-price context linking | UI test | Click article, verify context | ☐ |

### Verification Script

**Bash (Linux/macOS):**
```bash
#!/bin/bash
echo "=== Phase 3 Verification Script ==="

# 1. Gateway Health
echo "1. Checking Gateway..."
GATEWAY=$(curl -s http://localhost:8080/actuator/health)
if [[ $GATEWAY == *"UP"* ]]; then
    echo "   ✓ Gateway healthy"
else
    echo "   ✗ Gateway check failed"
fi

# 2. Route to Articles
echo "2. Testing article route..."
ARTICLES=$(curl -s http://localhost:8080/api/articles)
if [[ $ARTICLES == *"content"* ]]; then
    echo "   ✓ Article route working"
else
    echo "   ✗ Article route failed"
fi

# 3. Analysis Endpoint
echo "3. Checking NLP endpoint..."
ANALYSIS=$(curl -s http://localhost:8080/api/analysis/sentiment/bullish)
if [[ $ANALYSIS == *"content"* ]]; then
    echo "   ✓ NLP endpoint working"
else
    echo "   ⚠ NLP endpoint may have no data"
fi

# 4. Price Endpoint via Gateway
echo "4. Testing price route..."
PRICE=$(curl -s http://localhost:8080/api/prices/symbols)
if [[ $PRICE == *"BTC"* ]] || [[ $PRICE == *"["* ]]; then
    echo "   ✓ Price route working"
else
    echo "   ⚠ Price route may not be configured"
fi

echo "=== Verification Complete ==="
```

**PowerShell (Windows 10/11):**
```powershell
Write-Host "=== Phase 3 Verification Script ===" -ForegroundColor Cyan

# 1. Gateway Health
Write-Host "1. Checking Gateway..."
try {
    $gateway = Invoke-RestMethod -Uri http://localhost:8080/actuator/health -ErrorAction Stop
    if ($gateway.status -eq "UP") {
        Write-Host "   ✓ Gateway healthy" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Gateway check failed" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Gateway not responding" -ForegroundColor Red
}

# 2. Route to Articles
Write-Host "2. Testing article route..."
try {
    $articles = Invoke-RestMethod -Uri http://localhost:8080/api/articles -ErrorAction Stop
    Write-Host "   ✓ Article route working" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Article route failed" -ForegroundColor Red
}

# 3. Analysis Endpoint
Write-Host "3. Checking NLP endpoint..."
try {
    $analysis = Invoke-RestMethod -Uri "http://localhost:8080/api/analysis/sentiment/bullish" -ErrorAction Stop
    Write-Host "   ✓ NLP endpoint working" -ForegroundColor Green
} catch {
    Write-Host "   ⚠ NLP endpoint may have no data" -ForegroundColor Yellow
}

# 4. Price Endpoint
Write-Host "4. Testing price route..."
try {
    $price = Invoke-RestMethod -Uri http://localhost:8080/api/prices/symbols -ErrorAction Stop
    Write-Host "   ✓ Price route working" -ForegroundColor Green
} catch {
    Write-Host "   ⚠ Price route may not be configured" -ForegroundColor Yellow
}

Write-Host "=== Verification Complete ===" -ForegroundColor Cyan
```

---

## References

### Testing Documentation

- [Spring Cloud Gateway Testing](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#testing)
- [WireMock Documentation](https://wiremock.org/docs/)
- [Cypress Documentation](https://docs.cypress.io/)
- [Jest Documentation](https://jestjs.io/docs/getting-started)

### Related Project Documents

- [Phase3-ImplementationGuide.md](./Phase3-ImplementationGuide.md) - Implementation guide
- [Architecture.md](../Architecture.md) - System architecture
- [Phase4-TestingGuide.md](./Phase4-TestingGuide.md) - Next phase testing
