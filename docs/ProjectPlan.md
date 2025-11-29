# Project Plan — 9 Weeks (AI-Assisted Development)

**Timeline**: November 30, 2025 — January 31, 2026  
**Methodology**: Agile with 1-week sprints  
**AI Assistance**: Extensive use of LLMs for code generation, documentation, and problem-solving

> **Note**: For monitoring, security, Kubernetes, and CI/CD setup, see [Operations.md](./Operations.md).

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Phase Summary](#phase-summary)
3. [Detailed Sprint Plan](#detailed-sprint-plan)
4. [Scaling Considerations](#scaling-considerations)
5. [Risks & Mitigations](#risks--mitigations)
6. [Acceptance Criteria](#acceptance-criteria)

---

## Project Overview

### Goals
- Build a trading platform with news aggregation, real-time charts, and AI analysis
- Scale from 1 user to 1,000,000 concurrent users through evolutionary architecture
- Maintain code quality with AI-assisted development

### Tech Stack
| Category | Technology |
|----------|------------|
| Backend | Java 17+, Spring Boot 3.x |
| Frontend | Next.js, React, TradingView Charts |
| Databases | PostgreSQL, MongoDB, Redis |
| Messaging | Apache Kafka (Phase 4+) |
| Parsing | jsoup, Selenium |

---

## Phase Summary

| Phase | Weeks | Architecture | User Scale | Key Deliverables |
|-------|-------|--------------|------------|------------------|
| 1 | 1-2 | Monolith | 1-10 | Basic setup, single Spring Boot app |
| 2 | 3-4 | + Specialized DBs | 10-1,000 | MongoDB, Redis integration |
| 3 | 5-6 | Modular Monolith | 100-1,000 | API Gateway, service separation |
| 4 | 7-8 | Event-Driven | 1,000-10,000 | Kafka, async processing |
| 5 | 9 | Microservices-ready | 10,000+ | Independent services, documentation |

---

## Detailed Sprint Plan

### Week 1-2: Phase 1 — Monolithic Foundation

**Sprint 1 (Nov 30 - Dec 6)**
- Tasks:
  - [ ] Set up project structure (Maven multi-module optional)
  - [ ] Configure Docker Compose for PostgreSQL (dev environment)
  - [ ] Implement basic REST API skeleton
  - [ ] Set up Next.js frontend scaffold
- Deliverables:
  - Running Spring Boot application
  - Basic API endpoints (`/api/health`, `/api/version`)
  - Development environment documentation

**Sprint 2 (Dec 7 - Dec 13)**
- Tasks:
  - [ ] Implement basic Crawler module (jsoup for 1-2 sources)
  - [ ] Create Article entity and repository
  - [ ] Basic frontend layout with navigation
  - [ ] Implement scheduled crawling (@Scheduled)
- Deliverables:
  - Crawler collecting articles from 2 sources
  - Articles stored in PostgreSQL (JSONB)
  - Frontend displaying article list

**Rationale for Phase 1**:
> Starting with a monolith allows rapid iteration and debugging. All logic in one deployable unit means faster development cycles. PostgreSQL JSONB provides flexibility for semi-structured data without additional database complexity.

---

### Week 3-4: Phase 2 — Database Optimization

**Sprint 3 (Dec 14 - Dec 20)**
- Tasks:
  - [ ] Add MongoDB for raw article storage
  - [ ] Migrate article documents to MongoDB
  - [ ] Add Redis for caching
  - [ ] Implement cache-aside pattern for frequently accessed data
- Deliverables:
  - MongoDB storing raw and normalized articles
  - Redis caching article lists and metadata
  - Improved API response times

**Sprint 4 (Dec 21 - Dec 27)**
- Tasks:
  - [ ] Implement Price Collector (Binance API integration)
  - [ ] Store price ticks in PostgreSQL
  - [ ] Implement candle aggregation (1m, 5m, 15m, 1h, 1d)
  - [ ] Basic WebSocket for real-time price updates
- Deliverables:
  - Real-time price data from Binance
  - Price candles accessible via REST API
  - WebSocket streaming to frontend

**Rationale for Phase 2**:
> Specialized databases improve performance: MongoDB handles variable-schema articles efficiently, PostgreSQL optimizes for time-series price queries, and Redis reduces database load for read-heavy operations.

---

### Week 5-6: Phase 3 — Service Separation

**Sprint 5 (Dec 28 - Jan 3)**
- Tasks:
  - [ ] Introduce Spring Cloud Gateway
  - [ ] Define service module boundaries (api, crawler, price)
  - [ ] Implement NLP module (sentiment analysis using external API)
  - [ ] Store NLP results in MongoDB
- Deliverables:
  - API Gateway routing requests
  - Clear module separation in codebase
  - Basic sentiment analysis for articles

**Sprint 6 (Jan 4 - Jan 10)**
- Tasks:
  - [ ] Implement TradingView chart integration
  - [ ] Add multiple timeframe support
  - [ ] Link articles to price context (symbol, timestamp)
  - [ ] Frontend news feed with sentiment indicators
- Deliverables:
  - Interactive price charts with TradingView
  - News feed with sentiment scores
  - Article-chart context linking

**Rationale for Phase 3**:
> API Gateway provides a single entry point, simplifies client integration, and prepares for future load balancing. Module separation allows independent development and testing while maintaining single deployment.

---

### Week 7-8: Phase 4 — Async Processing

**Sprint 7 (Jan 11 - Jan 17)**
- Tasks:
  - [ ] Add Apache Kafka to Docker Compose
  - [ ] Refactor Crawler to publish to Kafka
  - [ ] Create Normalizer as Kafka consumer
  - [ ] Implement NLP Worker as Kafka consumer
- Deliverables:
  - Kafka-based event pipeline
  - Decoupled crawler → normalizer → NLP flow
  - Improved system resilience

**Sprint 8 (Jan 18 - Jan 24)**
- Tasks:
  - [ ] Price Collector publishing to Kafka
  - [ ] Aggregator consuming price ticks
  - [ ] Redis pub/sub for WebSocket updates
  - [ ] Load testing (100-1000 concurrent connections)
- Deliverables:
  - Full async pipeline for prices
  - WebSocket scaling with Redis
  - Load test results and optimizations

**Rationale for Phase 4**:
> Kafka enables horizontal scaling of workers, provides message durability, and decouples producers from consumers. This is essential for handling 1000+ concurrent users with variable processing loads.

---

### Week 9: Phase 5 — Microservices Preparation

**Sprint 9 (Jan 25 - Jan 31)**
- Tasks:
  - [ ] Document service contracts (OpenAPI specs)
  - [ ] Prepare Docker images for each service
  - [ ] Create deployment documentation
  - [ ] Final integration testing
  - [ ] Performance tuning and optimization
- Deliverables:
  - Production-ready Docker images
  - Complete API documentation
  - Deployment guide
  - Performance benchmarks

**Rationale for Phase 5**:
> Preparing for microservices deployment allows independent scaling of services. At 10,000+ users, specific services (e.g., WebSocket, NLP) may need to scale independently based on load patterns.

---

## Scaling Considerations

### User Scale Progression

| Users | Architecture | Database Strategy | Key Techniques |
|-------|--------------|-------------------|----------------|
| 1-10 | Monolith | Single PostgreSQL | N/A |
| 10-1,000 | Monolith + caching | PostgreSQL + MongoDB + Redis | Connection pooling, caching |
| 100-1,000 | Modular monolith | Same + read replicas | API Gateway, module separation |
| 1,000-10,000 | Event-driven | + Kafka | Async processing, worker scaling |
| 10,000-100,000 | Microservices | + TimescaleDB | Independent service scaling |
| 100,000-1,000,000 | CQRS/Event Sourcing | + Elasticsearch | CQRS, read/write separation |

### When to Upgrade Architecture

- **Phase 1 → 2**: When database becomes bottleneck (response time > 500ms)
- **Phase 2 → 3**: When codebase exceeds 50k LOC or 3+ developers
- **Phase 3 → 4**: When sync processing causes request timeouts
- **Phase 4 → 5**: When specific services need independent scaling
- **Phase 5 → 6**: When read/write patterns diverge significantly

---

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Captchas/Blocking | Medium | High | Headless browsers, proxy rotation, rate limiting |
| High-volume ticks | Medium | Medium | TimescaleDB, data partitioning, archiving |
| NLP accuracy | Low | Medium | Start with baseline models, switchable providers |
| Exchange API limits | Medium | High | Multiple exchange support, caching |
| Scope creep | High | Medium | Strict phase boundaries, weekly reviews |

---

## Acceptance Criteria

### Per-Phase Criteria

**Phase 1**:
- [ ] Application starts and responds to health check
- [ ] Crawler fetches articles from 2+ sources
- [ ] Articles displayed on frontend

**Phase 2**:
- [ ] MongoDB storing raw articles
- [ ] Redis cache hit ratio > 80%
- [ ] Price data updating in real-time

**Phase 3**:
- [ ] API Gateway routing all requests
- [ ] Sentiment analysis on all articles
- [ ] TradingView chart functional

**Phase 4**:
- [ ] Kafka pipeline processing messages
- [ ] System handles 1000 concurrent WebSocket connections
- [ ] No data loss on worker restart

**Phase 5**:
- [ ] Docker images for all services
- [ ] Complete API documentation
- [ ] Load test: 5000 concurrent users, < 500ms p95 latency

---

## Team Structure

| Role | Responsibility | Allocation |
|------|---------------|------------|
| Backend Lead | API, services, architecture | Full-time |
| Frontend Dev | Next.js, charts, UI | Full-time |
| Data Engineer | Crawler, NLP, data pipelines | Part-time |
| AI Assistant | Code generation, documentation, reviews | Continuous |

---

## References

- [Architecture.md](./Architecture.md) - Detailed architecture decisions
- [Features.md](./Features.md) - Feature specifications
- [CoreRequirements.md](./CoreRequirements.md) - Business requirements
- [Operations.md](./Operations.md) - Monitoring, CI/CD, and deployment