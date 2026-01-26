# Trading Platform Architecture - Technical Q&A

**Document Version**: 1.0  
**Date**: January 26, 2026  
**Author**: Technical Lead  
**Purpose**: Answer architecture questions for presentation/defense

---

## Table of Contents

1. [Question 1: Crawler Error Handling & Resilience](#question-1-crawler-error-handling--resilience)
2. [Question 2: VIP vs Non-VIP Resource Isolation](#question-2-vip-vs-non-vip-resource-isolation)
3. [Question 3: Architecture Clarification & Kubernetes Scaling](#question-3-architecture-clarification--kubernetes-scaling)

---

## Question 1: Crawler Error Handling & Resilience

### Original Question (Vietnamese)

> Vì thực hiện Crawl từ nhiều nguồn web tiền tệ khác nhau, có cấu trúc HTML khác nhau, và có thể thay đổi linh hoạt, không cố định theo thời gian, thì thực hiện Crawl có thể bị lỗi, làm sao để:
>
> - Hệ thống phát hiện lỗi thế nào trong quá trình Crawl
> - Sửa hay cập nhật thế nào để đảm bảo không gây tắc nghẽn hay sập toàn bộ hệ thống

### Answer

#### 1.1 Error Detection Strategies

**A. Structural Validation Layer**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    CRAWLER ERROR DETECTION FLOW                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────────────┐   │
│  │  Source  │───▶│   Fetcher    │───▶│  Structure Validator    │   │
│  │ Website  │    │  (HTTP/WS)   │    │  (Schema Comparison)    │   │
│  └──────────┘    └──────────────┘    └───────────┬─────────────┘   │
│                                                   │                  │
│                    ┌──────────────────────────────┼──────────────┐  │
│                    │                              ▼              │  │
│                    │    ┌─────────────────────────────────────┐ │  │
│                    │    │       VALIDATION CHECKS             │ │  │
│                    │    ├─────────────────────────────────────┤ │  │
│                    │    │ ✓ Expected fields present?          │ │  │
│                    │    │ ✓ Data types correct?               │ │  │
│                    │    │ ✓ Required selectors found?         │ │  │
│                    │    │ ✓ Content length reasonable?        │ │  │
│                    │    │ ✓ Timestamp/freshness valid?        │ │  │
│                    │    └──────────────┬──────────────────────┘ │  │
│                    │                   │                        │  │
│                    │         ┌─────────┴─────────┐              │  │
│                    │         ▼                   ▼              │  │
│                    │    ┌─────────┐         ┌─────────┐         │  │
│                    │    │  PASS   │         │  FAIL   │         │  │
│                    │    └────┬────┘         └────┬────┘         │  │
│                    │         │                   │              │  │
│                    │         ▼                   ▼              │  │
│                    │    ┌─────────┐         ┌─────────────┐     │  │
│                    │    │ Process │         │ Quarantine  │     │  │
│                    │    │  Data   │         │ + Alert     │     │  │
│                    │    └─────────┘         └─────────────┘     │  │
│                    └────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

**B. Error Detection Mechanisms**

| Detection Type               | Implementation              | Trigger Condition               |
| ---------------------------- | --------------------------- | ------------------------------- |
| **HTTP Status Monitoring**   | Check response codes        | 4xx, 5xx responses              |
| **Content Hash Comparison**  | Compare page structure hash | Hash differs >30% from baseline |
| **Selector Failure Rate**    | Track CSS/XPath success     | >20% selectors fail             |
| **Data Quality Scoring**     | Score extracted data        | Score < threshold (0.7)         |
| **Latency Anomaly**          | Monitor response times      | Response > 3x average           |
| **Empty Response Detection** | Check content length        | Body < minimum bytes            |

**C. Metrics & Alerting (Prometheus/Grafana)**

```yaml
# Key metrics to monitor
crawler_requests_total{source, status}      # Total requests per source
crawler_extraction_failures_total{source}   # Extraction failures
crawler_selector_success_rate{source}       # % of selectors working
crawler_data_quality_score{source}          # Quality score 0-1
crawler_circuit_breaker_state{source}       # open/closed/half-open
```

#### 1.2 Fault Isolation & Recovery Strategies

**A. Circuit Breaker Pattern (Resilience4j)**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     CIRCUIT BREAKER STATE MACHINE                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                         ┌──────────────┐                                 │
│                         │    CLOSED    │◄────────────────────┐           │
│                         │  (Normal)    │                     │           │
│                         └──────┬───────┘                     │           │
│                                │                             │           │
│                    Failure threshold                    Success in       │
│                    exceeded (5 failures)               HALF_OPEN         │
│                                │                             │           │
│                                ▼                             │           │
│                         ┌──────────────┐              ┌──────┴───────┐   │
│                         │     OPEN     │─────────────▶│  HALF_OPEN   │   │
│                         │  (Blocking)  │  After 60s   │  (Testing)   │   │
│                         └──────────────┘              └──────┬───────┘   │
│                                ▲                             │           │
│                                │                             │           │
│                                └─────────────────────────────┘           │
│                                      Failure in HALF_OPEN                │
│                                                                          │
│  Configuration:                                                          │
│  - failureRateThreshold: 50%                                            │
│  - waitDurationInOpenState: 60s                                         │
│  - permittedNumberOfCallsInHalfOpenState: 3                             │
│  - slidingWindowSize: 10                                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Implementation Example (Java/Spring)**:

```java
@CircuitBreaker(name = "crawlerService", fallbackMethod = "fallbackCrawl")
@Bulkhead(name = "crawlerService", type = Bulkhead.Type.THREADPOOL)
@Retry(name = "crawlerService")
public Article crawlSource(String sourceUrl) {
    // Crawl logic here
}

public Article fallbackCrawl(String sourceUrl, Exception e) {
    log.warn("Circuit breaker triggered for {}: {}", sourceUrl, e.getMessage());
    // Return cached data or empty response
    return cacheService.getLastKnownArticle(sourceUrl);
}
```

**B. Bulkhead Pattern (Source Isolation)**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        BULKHEAD ISOLATION                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│   │  CoinDesk Pool  │  │  Reuters Pool   │  │  Bloomberg Pool │         │
│   │  ┌───┐ ┌───┐    │  │  ┌───┐ ┌───┐    │  │  ┌───┐ ┌───┐    │         │
│   │  │ T │ │ T │    │  │  │ T │ │ T │    │  │  │ T │ │ T │    │         │
│   │  └───┘ └───┘    │  │  └───┘ └───┘    │  │  └───┘ └───┘    │         │
│   │  Max: 5 threads │  │  Max: 5 threads │  │  Max: 5 threads │         │
│   │  Queue: 10      │  │  Queue: 10      │  │  Queue: 10      │         │
│   └────────┬────────┘  └────────┬────────┘  └────────┬────────┘         │
│            │                    │                    │                   │
│            │     If one source fails, others        │                   │
│            │     continue operating normally         │                   │
│            │                    │                    │                   │
│            ▼                    ▼                    ▼                   │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                    Message Queue (RabbitMQ)                      │   │
│   │                    Dead Letter Queue for failures                │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Resilience4j Configuration (application.yml)**:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      crawlerService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowType: COUNT_BASED
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 60s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10

  bulkhead:
    instances:
      coindesk:
        maxConcurrentCalls: 5
        maxWaitDuration: 500ms
      reuters:
        maxConcurrentCalls: 5
        maxWaitDuration: 500ms
      bloomberg:
        maxConcurrentCalls: 5
        maxWaitDuration: 500ms

  retry:
    instances:
      crawlerService:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
```

**C. Graceful Degradation Strategy**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     GRACEFUL DEGRADATION FLOW                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   PRIMARY SOURCE                FALLBACK 1              FALLBACK 2       │
│   ┌─────────────┐              ┌─────────────┐        ┌─────────────┐   │
│   │  Live HTML  │──── FAIL ───▶│ Cached Data │─ FAIL ─▶│  Stale OK   │   │
│   │   Crawl     │              │  (Redis)    │        │ (Last Good) │   │
│   └─────────────┘              └─────────────┘        └─────────────┘   │
│         │                             │                      │          │
│         │ SUCCESS                     │ HIT                  │          │
│         ▼                             ▼                      ▼          │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                      Article Service                             │   │
│   │  - Mark data freshness (live/cached/stale)                      │   │
│   │  - Include staleness indicator in response                       │   │
│   │  - Log degradation events for monitoring                         │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**D. Dead Letter Queue (DLQ) for Failed Crawls**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    DEAD LETTER QUEUE ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐   │
│    │   Crawler    │────────▶│  Main Queue  │────────▶│   Consumer   │   │
│    │   Producer   │         │              │         │              │   │
│    └──────────────┘         └──────┬───────┘         └──────────────┘   │
│                                    │                                     │
│                           Processing Failed                              │
│                           (after 3 retries)                              │
│                                    │                                     │
│                                    ▼                                     │
│                          ┌──────────────────┐                            │
│                          │  Dead Letter     │                            │
│                          │  Queue (DLQ)     │                            │
│                          └────────┬─────────┘                            │
│                                   │                                      │
│              ┌────────────────────┼────────────────────┐                │
│              ▼                    ▼                    ▼                │
│    ┌──────────────┐     ┌──────────────┐     ┌──────────────┐          │
│    │  Alert Team  │     │   Log for    │     │   Manual     │          │
│    │  (PagerDuty) │     │   Analysis   │     │   Review     │          │
│    └──────────────┘     └──────────────┘     └──────────────┘          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 1.3 AI-Based Crawler Adaptation (Integration Points for Teammate)

Since the AI-based crawler is handled by another team member, here are integration recommendations:

**Recommended Architecture for AI Crawler:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    AI-ADAPTIVE CRAWLER ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    HTML Structure Learning                        │   │
│  │  ┌─────────────┐     ┌─────────────┐     ┌─────────────────┐    │   │
│  │  │   Sample    │────▶│  Feature    │────▶│   ML Model      │    │   │
│  │  │   Pages     │     │  Extraction │     │  (Transformer)  │    │   │
│  │  └─────────────┘     └─────────────┘     └────────┬────────┘    │   │
│  │                                                    │             │   │
│  │                                          ┌────────▼────────┐    │   │
│  │                                          │ Selector Rules  │    │   │
│  │                                          │   (Dynamic)     │    │   │
│  │                                          └────────┬────────┘    │   │
│  └───────────────────────────────────────────────────┼──────────────┘   │
│                                                      │                   │
│  ┌───────────────────────────────────────────────────▼──────────────┐   │
│  │                    Structure Change Detection                     │   │
│  │  ┌─────────────┐     ┌─────────────┐     ┌─────────────────┐    │   │
│  │  │  Current    │     │  Compare    │     │   Alert if      │    │   │
│  │  │  Structure  │────▶│  Baseline   │────▶│   Drift > 30%   │    │   │
│  │  └─────────────┘     └─────────────┘     └─────────────────┘    │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  Integration Endpoints (for teammate):                                   │
│  - POST /api/crawler/sources - Register new source                      │
│  - POST /api/crawler/selectors - Update selectors dynamically           │
│  - GET  /api/crawler/health/{source} - Get source health status         │
│  - POST /api/crawler/train - Trigger model retraining                   │
│                                                                          │
│  References:                                                             │
│  - Autoscraper: https://github.com/alirezamika/autoscraper              │
│  - Diffbot: https://www.diffbot.com/                                    │
│  - Trafilatura: https://trafilatura.readthedocs.io/                     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 1.4 References

1. **Circuit Breaker Pattern**: Nygard, M. (2018). _Release It!_ (2nd ed.). Pragmatic Bookshelf. Chapter 5.
2. **Resilience4j Documentation**: https://resilience4j.readme.io/docs/circuitbreaker
3. **Microsoft Cloud Design Patterns**: https://docs.microsoft.com/en-us/azure/architecture/patterns/circuit-breaker
4. **Netflix Hystrix (legacy but foundational)**: https://github.com/Netflix/Hystrix/wiki
5. **Bulkhead Pattern**: https://docs.microsoft.com/en-us/azure/architecture/patterns/bulkhead

---

## Question 2: VIP vs Non-VIP Resource Isolation

### Original Question (Vietnamese)

> Làm sao để yêu cầu phân tích và dự đoán của AI Service của người dùng VIP không gây ảnh hưởng lên trải nghiệm của người dùng không VIP (trong việc duy trì các kết nối để lấy thông tin mới nhất)?

### Answer

#### 2.1 Problem Analysis

VIP users require access to AI-powered analysis which is computationally expensive. This could:

- Consume shared resources (CPU, memory, network)
- Increase latency for all users
- Cause connection timeouts for real-time price streams

**Key Insight**: The real-time price streaming (WebSocket) must remain isolated from AI analysis workloads.

#### 2.2 Recommended Architecture: Queue-Based Priority Isolation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    VIP vs NON-VIP RESOURCE ISOLATION                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                            ┌──────────────────┐                              │
│                            │   API Gateway    │                              │
│                            │   (Rate Limit)   │                              │
│                            └────────┬─────────┘                              │
│                                     │                                        │
│                    ┌────────────────┼────────────────┐                      │
│                    │                │                │                      │
│                    ▼                ▼                ▼                      │
│     ┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐           │
│     │  Price Service   │  │ User Service │  │   AI Service     │           │
│     │  (WebSocket)     │  │              │  │   (VIP Only)     │           │
│     │  ALL USERS       │  │  ALL USERS   │  │                  │           │
│     └────────┬─────────┘  └──────────────┘  └────────┬─────────┘           │
│              │                                        │                     │
│              │                                        │                     │
│     ┌────────▼─────────┐                    ┌────────▼─────────┐           │
│     │    RabbitMQ      │                    │    RabbitMQ      │           │
│     │  price.updates   │                    │  ai.analysis     │           │
│     │  (High Priority) │                    │  (Low Priority)  │           │
│     └──────────────────┘                    └──────────────────┘           │
│                                                                              │
│  ═══════════════════════════════════════════════════════════════════════   │
│                        PHYSICAL ISOLATION                                    │
│  ═══════════════════════════════════════════════════════════════════════   │
│                                                                              │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │                     KUBERNETES CLUSTER                           │    │
│     │  ┌─────────────────────┐      ┌─────────────────────┐          │    │
│     │  │   Node Pool: CORE   │      │  Node Pool: AI      │          │    │
│     │  │   (price-service)   │      │  (ai-service)       │          │    │
│     │  │   (user-service)    │      │  GPU-enabled        │          │    │
│     │  │   High availability │      │  Burstable          │          │    │
│     │  └─────────────────────┘      └─────────────────────┘          │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.3 Implementation Strategies

**A. RabbitMQ Priority Queues**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    RABBITMQ QUEUE ARCHITECTURE                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Exchange: trading.exchange (topic)                                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │   Routing Keys:                                                   │   │
│  │   - price.* → price-updates-queue (priority: 10)                 │   │
│  │   - news.*  → news-updates-queue  (priority: 8)                  │   │
│  │   - ai.*    → ai-analysis-queue   (priority: 3)                  │   │
│  │                                                                   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  Queue Configuration:                                                    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  price-updates-queue:                                            │    │
│  │    x-max-priority: 10                                            │    │
│  │    x-message-ttl: 5000      # 5 seconds - real-time critical    │    │
│  │    prefetch: 100            # High throughput                    │    │
│  │                                                                   │    │
│  │  ai-analysis-queue:                                              │    │
│  │    x-max-priority: 10                                            │    │
│  │    x-message-ttl: 300000    # 5 minutes - can wait              │    │
│  │    prefetch: 1              # Process one at a time             │    │
│  │    x-dead-letter-exchange: ai.dlx                               │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**RabbitMQ Configuration (Java)**:

```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue priceUpdatesQueue() {
        return QueueBuilder.durable("price-updates-queue")
            .withArgument("x-max-priority", 10)
            .withArgument("x-message-ttl", 5000)
            .build();
    }

    @Bean
    public Queue aiAnalysisQueue() {
        return QueueBuilder.durable("ai-analysis-queue")
            .withArgument("x-max-priority", 10)
            .withArgument("x-message-ttl", 300000)
            .withArgument("x-dead-letter-exchange", "ai.dlx")
            .build();
    }

    @Bean
    public TopicExchange tradingExchange() {
        return new TopicExchange("trading.exchange");
    }

    @Bean
    public Binding priceBinding(Queue priceUpdatesQueue, TopicExchange exchange) {
        return BindingBuilder.bind(priceUpdatesQueue)
            .to(exchange)
            .with("price.*");
    }

    @Bean
    public Binding aiBinding(Queue aiAnalysisQueue, TopicExchange exchange) {
        return BindingBuilder.bind(aiAnalysisQueue)
            .to(exchange)
            .with("ai.*");
    }
}
```

**B. Rate Limiting by User Tier (API Gateway)**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    RATE LIMITING CONFIGURATION                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   User Tier       │ Price API │ News API │ AI Analysis │ WebSocket     │
│   ────────────────┼───────────┼──────────┼─────────────┼───────────    │
│   REGULAR         │ 100/min   │ 50/min   │ BLOCKED     │ 1 connection  │
│   VIP             │ 500/min   │ 200/min  │ 10/min      │ 5 connections │
│   ADMIN           │ Unlimited │ Unlimited│ Unlimited   │ Unlimited     │
│                                                                          │
│   Implementation: Spring Cloud Gateway + Redis                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**API Gateway Rate Limiting (application.yml)**:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: price-service
          uri: lb://price-service
          predicates:
            - Path=/api/prices/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: '#{@userKeyResolver}'

        - id: ai-service-vip
          uri: lb://ai-service
          predicates:
            - Path=/api/ai/**
            - Header=X-User-Role, VIP|ADMIN
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

**C. Kubernetes Resource Quotas & Node Affinity**

```yaml
# Core services node pool (high priority)
apiVersion: v1
kind: ResourceQuota
metadata:
  name: core-services-quota
  namespace: trading-core
spec:
  hard:
    requests.cpu: '8'
    requests.memory: 16Gi
    limits.cpu: '16'
    limits.memory: 32Gi

---
# AI services node pool (burstable, lower priority)
apiVersion: v1
kind: ResourceQuota
metadata:
  name: ai-services-quota
  namespace: trading-ai
spec:
  hard:
    requests.cpu: '4'
    requests.memory: 8Gi
    limits.cpu: '8'
    limits.memory: 16Gi
```

#### 2.4 Request Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REQUEST FLOW BY USER TYPE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  REGULAR USER FLOW:                                                          │
│  ┌──────┐    ┌─────────┐    ┌──────────────┐    ┌──────────────┐           │
│  │Client│───▶│ Gateway │───▶│Price Service │◀──▶│  RabbitMQ    │           │
│  │      │◀───│         │◀───│  (WebSocket) │    │ price.updates│           │
│  └──────┘    └─────────┘    └──────────────┘    └──────────────┘           │
│     │                                                                        │
│     │  ❌ AI endpoints blocked (403 Forbidden)                              │
│     │                                                                        │
│  ═══════════════════════════════════════════════════════════════════════    │
│                                                                              │
│  VIP USER FLOW:                                                              │
│  ┌──────┐    ┌─────────┐    ┌──────────────┐    ┌──────────────┐           │
│  │Client│───▶│ Gateway │───▶│Price Service │◀──▶│  RabbitMQ    │           │
│  │      │◀───│         │◀───│  (WebSocket) │    │ price.updates│           │
│  └──────┘    └────┬────┘    └──────────────┘    └──────────────┘           │
│     │             │                                                          │
│     │             │ (Async - separate thread pool)                          │
│     │             ▼                                                          │
│     │        ┌─────────┐    ┌──────────────┐    ┌──────────────┐           │
│     │        │   AI    │───▶│  AI Service  │───▶│  RabbitMQ    │           │
│     └────────│ Request │◀───│  (Analysis)  │◀───│ ai.analysis  │           │
│              └─────────┘    └──────────────┘    └──────────────┘           │
│                                                                              │
│  Key: AI requests are processed asynchronously and don't block              │
│       real-time price streaming for any user                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.5 Benefits of This Architecture

| Aspect                   | Benefit                                           |
| ------------------------ | ------------------------------------------------- |
| **Physical Isolation**   | AI workloads on separate K8s node pool            |
| **Queue Priority**       | Price updates always processed first              |
| **Rate Limiting**        | Prevents any single user from overwhelming system |
| **Async Processing**     | AI analysis doesn't block synchronous operations  |
| **Graceful Degradation** | If AI service is slow, core services unaffected   |
| **Cost Efficiency**      | AI node pool can use spot/preemptible instances   |

---

## Question 3: Architecture Clarification & Kubernetes Scaling

### Original Question (Vietnamese)

> Đính chính một cách thẳng thắn các thông tin sau:
>
> - Hiện tại hệ thống là có một PriceCollector chuyên lấy về các giá mới nhất từ API, và publish xuống RabbitMQ, đồng thời ghi vào một PriceDB (MongoDB) đúng không?
> - Khi áp dụng thêm K8S để autoscale các pod chạy các PriceService...

### Answer

#### 3.1 Current Architecture Clarification

**Q: Is there a PriceCollector that fetches prices from API, publishes to RabbitMQ, and writes to PriceDB?**

**A: YES, with clarifications:**

Based on code analysis:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CURRENT PRICE DATA FLOW                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                    PRICE COLLECTOR                               │   │
│   │   (price-service with SPRING_PROFILES_ACTIVE=collector)         │   │
│   │                                                                   │   │
│   │   ┌─────────────┐                                               │   │
│   │   │ Binance WS  │──┐                                            │   │
│   │   │ (Kline)     │  │    ┌───────────────┐    ┌────────────┐    │   │
│   │   └─────────────┘  ├───▶│TickBufferSvc │───▶│ PostgreSQL │    │   │
│   │   ┌─────────────┐  │    │              │    │ (PriceDB)  │    │   │
│   │   │ Binance API │──┘    └───────┬──────┘    └────────────┘    │   │
│   │   │ (Historical)│               │                              │   │
│   │   └─────────────┘               │                              │   │
│   │                                 ▼                              │   │
│   │                         ┌─────────────┐                        │   │
│   │                         │  RabbitMQ   │                        │   │
│   │                         │  (STOMP)    │                        │   │
│   │                         └─────────────┘                        │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│   Note: PriceDB is PostgreSQL, not MongoDB (MongoDB is for UserService) │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Key Components Identified:**

| Component               | File                         | Purpose                         |
| ----------------------- | ---------------------------- | ------------------------------- |
| `PriceCollectorService` | price-service/.../collector/ | Fetches from Binance WebSocket  |
| `TickBufferService`     | price-service/.../service/   | Buffers and batches price ticks |
| `PriceTickSaver`        | price-service/.../service/   | Persists to PostgreSQL          |
| `RabbitMQ Publisher`    | Configured via STOMP         | Publishes price updates         |

**Database Clarification:**

- **PostgreSQL**: Price data (PriceCandle, PriceTick entities)
- **MongoDB**: User data (User, RefreshToken, Articles)

#### 3.2 Kubernetes Scaling Clarification

**Q: When applying K8s autoscaling, each PriceService maintains 1 WebSocket to RabbitMQ, and N end-users each have 1 WebSocket to PriceService?**

**A: YES, this is correct:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    WEBSOCKET CONNECTION MODEL                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                         ┌─────────────────────┐                         │
│                         │     RabbitMQ        │                         │
│                         │   (STOMP Broker)    │                         │
│                         └──────────┬──────────┘                         │
│                                    │                                     │
│              ┌─────────────────────┼─────────────────────┐              │
│              │                     │                     │              │
│              ▼                     ▼                     ▼              │
│    ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐     │
│    │ PriceService    │   │ PriceService    │   │ PriceService    │     │
│    │ Pod 1           │   │ Pod 2           │   │ Pod N           │     │
│    │ ┌─────────────┐ │   │ ┌─────────────┐ │   │ ┌─────────────┐ │     │
│    │ │ 1 STOMP     │ │   │ │ 1 STOMP     │ │   │ │ 1 STOMP     │ │     │
│    │ │ Connection  │ │   │ │ Connection  │ │   │ │ Connection  │ │     │
│    │ └─────────────┘ │   │ └─────────────┘ │   │ └─────────────┘ │     │
│    └────────┬────────┘   └────────┬────────┘   └────────┬────────┘     │
│             │                     │                     │              │
│    ┌────────┴────────┐   ┌────────┴────────┐   ┌────────┴────────┐     │
│    │    Users 1-K    │   │  Users K+1-2K   │   │ Users (N-1)K-NK │     │
│    │  ┌───┐ ┌───┐    │   │  ┌───┐ ┌───┐    │   │  ┌───┐ ┌───┐    │     │
│    │  │WS │ │WS │... │   │  │WS │ │WS │... │   │  │WS │ │WS │... │     │
│    │  └───┘ └───┘    │   │  └───┘ └───┘    │   │  └───┘ └───┘    │     │
│    └─────────────────┘   └─────────────────┘   └─────────────────┘     │
│                                                                          │
│    Each Pod: 1 connection to RabbitMQ, many connections from users      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 3.3 Recommended Metrics for Autoscaling

**Q: What metrics should be used for K8s autoscaling?**

**A: For WebSocket-heavy services, custom metrics are essential:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    AUTOSCALING METRICS MATRIX                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    PRIMARY METRICS                               │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │                                                                   │    │
│  │  Metric                          │ Target    │ Scale Trigger     │    │
│  │  ────────────────────────────────┼───────────┼─────────────────  │    │
│  │  websocket_connections_active    │ 500/pod   │ >80% → scale up   │    │
│  │  cpu_utilization                 │ 70%       │ >70% → scale up   │    │
│  │  memory_utilization              │ 80%       │ >80% → scale up   │    │
│  │  message_processing_latency_p99  │ 100ms     │ >100ms → scale up │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                   SECONDARY METRICS                              │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │                                                                   │    │
│  │  Metric                          │ Purpose                       │    │
│  │  ────────────────────────────────┼─────────────────────────────  │    │
│  │  rabbitmq_queue_depth            │ Backpressure indicator        │    │
│  │  websocket_messages_per_second   │ Throughput monitoring         │    │
│  │  gc_pause_duration               │ JVM health                    │    │
│  │  thread_pool_active_threads      │ Concurrency saturation        │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Recommended HPA Configuration:**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: price-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: price-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    # Custom metric: WebSocket connections
    - type: Pods
      pods:
        metric:
          name: websocket_connections_active
        target:
          type: AverageValue
          averageValue: '400' # Scale when avg > 400 connections/pod

    # Standard CPU metric
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70

    # Standard Memory metric
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80

  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 25
          periodSeconds: 120
```

#### 3.4 Load Balancer Requirements

**Q: Do we need a Load Balancer for WebSocket services? What functions should it perform?**

**A: YES, with specific WebSocket considerations:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    LOAD BALANCER ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                          ┌─────────────────────┐                        │
│                          │    NGINX Ingress    │                        │
│                          │    Controller       │                        │
│                          └──────────┬──────────┘                        │
│                                     │                                    │
│   ┌─────────────────────────────────┼─────────────────────────────────┐ │
│   │                                 │                                  │ │
│   │   REQUIRED FUNCTIONS:           │                                  │ │
│   │                                 │                                  │ │
│   │   1. WebSocket Upgrade Support  │                                  │ │
│   │      - HTTP → WS protocol       │                                  │ │
│   │      - Connection: Upgrade      │                                  │ │
│   │                                 │                                  │ │
│   │   2. Sticky Sessions            │                                  │ │
│   │      - IP Hash or Cookie-based  │                                  │ │
│   │      - Critical for WS state    │                                  │ │
│   │                                 │                                  │ │
│   │   3. Health Checks              │                                  │ │
│   │      - /actuator/health         │                                  │ │
│   │      - Remove unhealthy pods    │                                  │ │
│   │                                 │                                  │ │
│   │   4. Connection Timeouts        │                                  │ │
│   │      - proxy_read_timeout 3600s │                                  │ │
│   │      - Keep long-lived WS open  │                                  │ │
│   │                                 │                                  │ │
│   └─────────────────────────────────┼─────────────────────────────────┘ │
│                                     │                                    │
│                    ┌────────────────┼────────────────┐                  │
│                    ▼                ▼                ▼                  │
│          ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│          │PriceService 1│  │PriceService 2│  │PriceService N│          │
│          └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**NGINX Ingress Configuration for WebSocket:**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: price-service-ingress
  annotations:
    nginx.ingress.kubernetes.io/proxy-read-timeout: '3600'
    nginx.ingress.kubernetes.io/proxy-send-timeout: '3600'
    nginx.ingress.kubernetes.io/upstream-hash-by: '$remote_addr'
    nginx.ingress.kubernetes.io/affinity: 'cookie'
    nginx.ingress.kubernetes.io/session-cookie-name: 'PRICE_SERVICE_AFFINITY'
    nginx.ingress.kubernetes.io/session-cookie-expires: '172800'
    nginx.ingress.kubernetes.io/session-cookie-max-age: '172800'
    nginx.ingress.kubernetes.io/websocket-services: 'price-service'
spec:
  ingressClassName: nginx
  rules:
    - host: api.trading.example.com
      http:
        paths:
          - path: /ws/prices
            pathType: Prefix
            backend:
              service:
                name: price-service
                port:
                  number: 8083
```

#### 3.5 Additional Load Balancer Filters (Project-Specific)

**Q: Should the Load Balancer do more than load balancing in this project context?**

**A: YES, these additional filters are recommended:**

| Filter              | Purpose                     | Implementation               |
| ------------------- | --------------------------- | ---------------------------- |
| **JWT Validation**  | Verify token before routing | NGINX auth_request           |
| **Rate Limiting**   | Per-user connection limits  | nginx limit_conn             |
| **IP Whitelisting** | Admin endpoints protection  | nginx allow/deny             |
| **Request Logging** | Audit trail                 | Access logs + correlation ID |
| **TLS Termination** | HTTPS/WSS security          | SSL certificates             |
| **CORS Handling**   | Cross-origin WebSocket      | Headers configuration        |

```nginx
# Additional NGINX configuration for project-specific filters
location /ws/prices {
    # Rate limiting per IP
    limit_conn addr 10;

    # JWT validation (via auth service)
    auth_request /auth/validate;
    auth_request_set $user_id $upstream_http_x_user_id;
    auth_request_set $user_role $upstream_http_x_user_role;

    # Pass user info to upstream
    proxy_set_header X-User-Id $user_id;
    proxy_set_header X-User-Role $user_role;

    # WebSocket upgrade
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    proxy_pass http://price-service-upstream;
}
```

#### 3.6 Architecture Advantages & Disadvantages

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE EVALUATION                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ADVANTAGES:                                                             │
│  ┌────────────────────────────────────────────────────────────────┐     │
│  │ ✅ Horizontal Scalability                                       │     │
│  │    - Can scale PriceService pods independently                 │     │
│  │    - No single point of failure for WebSocket connections      │     │
│  │                                                                 │     │
│  │ ✅ Decoupled Data Flow                                          │     │
│  │    - PriceCollector isolated from PriceService                 │     │
│  │    - RabbitMQ provides backpressure handling                   │     │
│  │                                                                 │     │
│  │ ✅ Resilience                                                   │     │
│  │    - Pod failure doesn't lose all user connections             │     │
│  │    - Clients can reconnect to healthy pods                     │     │
│  │                                                                 │     │
│  │ ✅ Cost Efficiency                                              │     │
│  │    - Scale down during low traffic periods                     │     │
│  │    - Pay for what you use (cloud)                              │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                          │
│  DISADVANTAGES:                                                          │
│  ┌────────────────────────────────────────────────────────────────┐     │
│  │ ⚠️ Sticky Session Complexity                                    │     │
│  │    - WebSocket state tied to specific pod                      │     │
│  │    - Pod termination requires client reconnection              │     │
│  │    Mitigation: Graceful shutdown + client reconnect logic      │     │
│  │                                                                 │     │
│  │ ⚠️ Connection Rebalancing                                       │     │
│  │    - New pods don't automatically receive existing connections │     │
│  │    - May have uneven distribution after scaling                │     │
│  │    Mitigation: Periodic connection cycling (frontend)          │     │
│  │                                                                 │     │
│  │ ⚠️ Observability Complexity                                     │     │
│  │    - Tracking user across multiple pods                        │     │
│  │    - Distributed tracing required                              │     │
│  │    Mitigation: Correlation IDs, centralized logging            │     │
│  │                                                                 │     │
│  │ ⚠️ RabbitMQ as Single Point                                     │     │
│  │    - All price updates flow through RabbitMQ                   │     │
│  │    Mitigation: RabbitMQ cluster, HA configuration              │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 3.7 Complete Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              COMPLETE SYSTEM ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│   EXTERNAL                                                                               │
│   ┌─────────────┐     ┌─────────────┐                                                   │
│   │ Binance WS  │     │ Binance API │                                                   │
│   └──────┬──────┘     └──────┬──────┘                                                   │
│          │                   │                                                           │
│          └─────────┬─────────┘                                                           │
│                    ▼                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                           KUBERNETES CLUSTER                                     │   │
│   │  ┌────────────────────────────────────────────────────────────────────────────┐ │   │
│   │  │                         INGRESS (NGINX)                                     │ │   │
│   │  │   - TLS Termination  - Rate Limiting  - Sticky Sessions  - JWT Validation  │ │   │
│   │  └───────────────────────────────┬────────────────────────────────────────────┘ │   │
│   │                                  │                                               │   │
│   │  ┌───────────────────────────────▼────────────────────────────────────────────┐ │   │
│   │  │                         API GATEWAY (8081)                                  │ │   │
│   │  │   - Route Management  - Service Discovery  - Circuit Breaker               │ │   │
│   │  └───────────────────────────────┬────────────────────────────────────────────┘ │   │
│   │                                  │                                               │   │
│   │         ┌────────────────────────┼────────────────────────┐                     │   │
│   │         │                        │                        │                     │   │
│   │         ▼                        ▼                        ▼                     │   │
│   │  ┌─────────────┐         ┌─────────────┐         ┌─────────────────┐           │   │
│   │  │   User      │         │   Price     │         │  Price          │           │   │
│   │  │   Service   │         │   Service   │         │  Collector      │           │   │
│   │  │   (8082)    │         │   (8083)    │         │  (collector)    │           │   │
│   │  │   Replicas:1│         │   Replicas: │         │  Replicas: 1    │           │   │
│   │  │             │         │   2-10 (HPA)│         │  (Singleton)    │           │   │
│   │  └──────┬──────┘         └──────┬──────┘         └────────┬────────┘           │   │
│   │         │                       │                         │                     │   │
│   │         │                       │◀────────────────────────┤                     │   │
│   │         │                       │      (STOMP/AMQP)       │                     │   │
│   │         │                       │                         │                     │   │
│   │         │                ┌──────▼──────┐                  │                     │   │
│   │         │                │  RabbitMQ   │◀─────────────────┘                     │   │
│   │         │                │  (Cluster)  │                                        │   │
│   │         │                └─────────────┘                                        │   │
│   │         │                                                                       │   │
│   │         ▼                        ▼                                              │   │
│   │  ┌─────────────┐         ┌─────────────┐                                       │   │
│   │  │  MongoDB    │         │ PostgreSQL  │                                       │   │
│   │  │  (Users)    │         │  (Prices)   │                                       │   │
│   │  └─────────────┘         └─────────────┘                                       │   │
│   │                                                                                 │   │
│   │  ┌────────────────────────────────────────────────────────────────────────────┐ │   │
│   │  │                      DISCOVERY SERVER (Eureka 8761)                        │ │   │
│   │  └────────────────────────────────────────────────────────────────────────────┘ │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│   CLIENTS                                                                                │
│   ┌─────────┐  ┌─────────┐  ┌─────────┐                                                │
│   │ Browser │  │ Mobile  │  │  API    │                                                │
│   │  (WS)   │  │  App    │  │ Client  │                                                │
│   └─────────┘  └─────────┘  └─────────┘                                                │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Summary & Recommendations

### Key Takeaways

1. **Crawler Resilience**: Implement Circuit Breaker + Bulkhead patterns using Resilience4j to isolate failures per source and prevent cascade failures.

2. **VIP Isolation**: Use separate RabbitMQ queues with priority, rate limiting at API Gateway, and physical node pool separation in Kubernetes for AI workloads.

3. **Kubernetes Scaling**: Focus on custom metrics (WebSocket connections) alongside CPU/memory. Use NGINX Ingress with sticky sessions for WebSocket affinity.

4. **Load Balancer**: Essential for WebSocket services with specific configuration for connection upgrades, timeouts, and session affinity.

### Next Steps

1. ➡️ See `todo_list_part_5.md` for implementation tasks
2. ➡️ See `kubernetes_autoscaling_plan.md` for detailed K8s deployment manifests

---

## References

1. Nygard, M. (2018). _Release It!_ (2nd ed.). Pragmatic Bookshelf.
2. Newman, S. (2021). _Building Microservices_ (2nd ed.). O'Reilly Media.
3. Richardson, C. (2018). _Microservices Patterns_. Manning Publications.
4. Kubernetes Documentation: https://kubernetes.io/docs/
5. RabbitMQ Documentation: https://www.rabbitmq.com/documentation.html
6. Resilience4j: https://resilience4j.readme.io/
7. NGINX Ingress Controller: https://kubernetes.github.io/ingress-nginx/
