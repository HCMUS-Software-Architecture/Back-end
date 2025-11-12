# Project Plan — 12 weeks (Java stack)

Sprint length: 2 weeks. Total 6 sprints.

## Sprint 0 — Setup & Architecture (Weeks 1-2)
- Tasks:
  - Set up repository skeleton, CI (GitHub Actions), and a Docker Compose development stack.
  - Provision development databases: Postgres (optionally Timescale), MongoDB, Redis.
  - Create Spring Boot skeleton (API module) and a local Kafka broker for development.
- Deliverable: development stack brought up with `docker-compose up`.

## Sprint 1 — Basic Crawler & Storage (Weeks 3-4)
- Tasks:
  - Implement a modular crawler for 3 sources using Java (jsoup; Selenium where necessary).
  - Persist raw HTML to MongoDB and publish messages to Kafka.
  - Schedule crawler jobs (Quartz).
- Deliverable: crawling pipeline that stores raw pages and emits events; basic metrics.

## Sprint 2 — Normalizer & NLP pipeline (Weeks 5-6)
- Tasks:
  - Implement Normalizer service (Spring Boot consumer) to parse HTML and produce normalized JSON.
  - Integrate basic NLP: language detection and sentiment (library or external API).
  - Persist `analysis_results` to MongoDB.
- Deliverable: normalized articles and analysis stored in DB.

## Sprint 3 — Price Collector & Aggregation (Weeks 7-8)
- Tasks:
  - Implement price collector connecting to an exchange (API/WebSocket) and store ticks in TimescaleDB/Postgres.
  - Implement Aggregator service to generate candles for common intervals and store them.
  - Expose REST endpoints for historical candles.
- Deliverable: price ingestion pipeline and historical data endpoints.

## Sprint 4 — Frontend Chart & Realtime (Weeks 9-10)
- Tasks:
  - Build React frontend with charting (TradingView or Lightweight Charts) and WebSocket client.
  - Integrate news feed and link article details to chart symbols/time contexts.
- Deliverable: interactive chart UI and news feed integration.

## Sprint 5 — Integration, Tests & Deployment (Weeks 11-12)
- Tasks:
  - End-to-end tests, load testing, and performance tuning.
  - Build Docker images, create Kubernetes manifests (basic), and configure monitoring.
  - Produce documentation and runbook.
- Deliverable: production-ready deployment artifacts and documentation.

## Risks & Mitigations
- Captchas / Blocking: use headless browsers, proxy rotation, and rate limiting.
- High-volume ticks: use TimescaleDB or partitioning and archiving strategies.
- NLP accuracy: start with baseline models and make it possible to switch providers.

## Acceptance criteria (examples)
- Crawler: > 90% of scheduled pages crawled successfully for supported sources.
- Chart latency: new tick delivered to client < 1s.
- NLP: sentiment baseline > 70% on a development validation set.

## Team & Estimates
- Backend/Infra: 2 developers (1 backend lead + 1 DevOps part-time)
- Frontend: 1 React developer
- Data/AI: 1 data scientist (part-time)

---

If you want, I can expand this plan into a detailed task list (Jira-style), add definitions of done for each ticket, or scaffold a Spring Boot multi-module repository following this plan.