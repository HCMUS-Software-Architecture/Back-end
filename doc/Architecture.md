# Architecture Overview

Purpose: This system ingests financial news from multiple sources, stores raw and processed data, runs AI/NLP analysis, and provides realtime price charts similar to TradingView.

## Architecture summary (Java stack)

- Crawler/Extractor: Java (jsoup for HTML parsing, Selenium for dynamic pages). Deployed as workers (Spring Boot apps or standalone JVM processes). Scheduler: Quartz or Kubernetes CronJob.

- Message broker: Apache Kafka (streaming between crawler → normalizer → NLP → aggregator).

- Processing/Workers: Spring Boot microservices (Normalizer, NLP Worker, Aggregator). Workers scale as Kafka consumer groups.
- Storage:

  - PostgreSQL (+ TimescaleDB extension) for relational and time-series data (price ticks/candles).

  - MongoDB for raw articles and NLP results (document store).
  - Redis for cache and pub/sub bridging to WebSocket server.
- Backend API: Spring Boot (REST + WebSocket). Exposes endpoints: `/api/news`, `/api/prices/historical`, `/ws/prices`.
- Frontend: React (SPA). Charting: TradingView Charting Library or Lightweight Charts. Realtime via WebSocket.
- Observability: Prometheus + Grafana for metrics; ELK/EFK for logs; Zipkin/Jaeger for tracing.

## High-level data flow

1. The crawler fetches source pages → pushes raw HTML messages to Kafka topic `raw-articles` and stores raw documents in MongoDB `raw_articles`.

2. The Normalizer (consumer) reads `raw-articles`, parses and normalizes them into a common schema, stores `articles` in MongoDB and publishes `normalized-articles` to Kafka.
3. The NLP Worker (consumer) consumes `normalized-articles`, runs language detection, NER, sentiment, topic modeling and causal signal detection → stores `analysis_results` in MongoDB and may publish `analysis-events` for alerts.
4. The Price Collector connects to exchange APIs/WebSockets → stores ticks into TimescaleDB/Postgres (`price_ticks`) and publishes ticks to Kafka `price-ticks`.
5. The Aggregator (consumer) reads ticks → generates candles for configured intervals (1m,5m,...) → stores `price_candles` in Postgres/Timescale and updates Redis cache / publishes realtime updates via Redis pub/sub.
6. The Backend API serves frontend requests by reading from Postgres/Mongo/Redis; a WebSocket server subscribes to Redis channels to broadcast realtime updates to clients.

## Non-functional requirements

- Realtime latency: target <1s from tick to client.

- Scalability: Crawler, NLP, and Aggregator must scale horizontally using Kafka consumer groups.
- Idempotency: Crawl/normalize steps must dedupe using URL/hash to avoid duplicates.
- Data retention & backup: raw_articles retained for configurable period; aggregated candles archived as required; backup strategies for PostgreSQL and MongoDB.

## Suggested technology stack

- Language/Framework: Java 17+, Spring Boot, Spring Kafka.

- Parsing: jsoup, Selenium (headless) when necessary.
- Message broker: Kafka.
- Databases: PostgreSQL with TimescaleDB, MongoDB, Redis.
- Observability: Prometheus, Grafana, ELK, Zipkin/Jaeger.
- Orchestration: Docker Compose for development; Kubernetes for production.



## Diagrams (Mermaid)

### System-level architecture

```mermaid
graph LR
  subgraph Ingestion
    Crawler["Crawler (jsoup / Selenium)"]
    PriceCollector["Price Collector (API/WS)"]
  end

  subgraph Messaging
    Kafka["Kafka cluster"]
    Redis["Redis (cache/pubsub)"]
  end

  subgraph Processing
    Normalizer["Normalizer Service (Spring Boot)"]
    NLP["NLP Worker (Java/Python)"]
    Aggregator["Aggregator (candles)"]
  end

  subgraph Storage
    Mongo["MongoDB (raw/articles/analysis)"]
    Postgres["Postgres (+TimescaleDB) (ticks/candles)"]
  end

  subgraph Backend
    API["Backend API (Spring Boot)\nREST + WebSocket"]
    WebUI["Frontend (React)\nCharts + Dashboard"]
  end

  Crawler -->|raw-articles| Kafka
  PriceCollector -->|price-ticks| Kafka
  Kafka --> Normalizer
  Normalizer --> Mongo
  Normalizer --> Kafka
  Kafka --> NLP
  NLP --> Mongo
  Kafka --> Aggregator
  Aggregator --> Postgres
  Aggregator --> Redis
  Redis --> API
  API --> Postgres
  API --> Mongo
  WebUI --> API
  WebUI -->|ws| API

  style Kafka fill:#f9f,stroke:#333,stroke-width:1px
  style Redis fill:#fbf0b6,stroke:#333,stroke-width:1px
```

### Dataflow (detailed)

```mermaid
flowchart LR
  subgraph Crawl
    S[Scheduler] --> CR[Crawler]
    CR --> EX[Extractor]
    EX --> M1[MongoDB: raw_articles]
    EX --> K1[Kafka: raw-articles]
  end

  subgraph Normalize
    K1 --> N[Normalizer]
    N --> MA[MongoDB: articles]
    N --> K2[Kafka: normalized-articles]
  end

  subgraph NLP
    K2 --> W[NLP Worker]
    W --> AR[MongoDB: analysis_results]
    W -->|alerts| K3[Kafka: analysis-events]
  end

  subgraph Price
    PC[Price Collector] --> Kp[Kafka: price-ticks]
    Kp --> AG[Aggregator]
    AG --> PG[Postgres/Timescale: price_candles]
    AG --> Rd[Redis: recent_candles]
    Rd --> WS[WebSocket Server]
  end

  subgraph BackendAPI
    API -->|query| PG
    API -->|query| MA
    API -->|publish| WS
  end

  Web[Frontend] -->|REST/WS| API

```




