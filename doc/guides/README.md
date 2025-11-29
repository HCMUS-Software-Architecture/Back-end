# Implementation & Testing Guides Index

This directory contains comprehensive implementation and testing guides for each phase of the Trading Platform development.

---

## Quick Reference

| Phase | Implementation Guide | Testing Guide | Duration |
|-------|---------------------|---------------|----------|
| Phase 1 | [Phase1-ImplementationGuide.md](./Phase1-ImplementationGuide.md) | [Phase1-TestingGuide.md](./Phase1-TestingGuide.md) | Weeks 1-2 |
| Phase 2 | [Phase2-ImplementationGuide.md](./Phase2-ImplementationGuide.md) | [Phase2-TestingGuide.md](./Phase2-TestingGuide.md) | Weeks 3-4 |
| Phase 3 | [Phase3-ImplementationGuide.md](./Phase3-ImplementationGuide.md) | [Phase3-TestingGuide.md](./Phase3-TestingGuide.md) | Weeks 5-6 |
| Phase 4 | [Phase4-ImplementationGuide.md](./Phase4-ImplementationGuide.md) | [Phase4-TestingGuide.md](./Phase4-TestingGuide.md) | Weeks 7-8 |
| Phase 5 | [Phase5-ImplementationGuide.md](./Phase5-ImplementationGuide.md) | [Phase5-TestingGuide.md](./Phase5-TestingGuide.md) | Week 9 |

---

## Special Guides

| Guide | Description |
|-------|-------------|
| [Frontend-ResearchGuide.md](./Frontend-ResearchGuide.md) | Comprehensive frontend research, libraries, and implementation strategies |

---

## Phase Summaries

### Phase 1: Monolithic Foundation (1-10 users)
- **Key Deliverables**: Spring Boot app, PostgreSQL, Basic Crawler
- **Technologies**: Java 17, Spring Boot 3.x, jsoup, H2/PostgreSQL
- [Implementation Guide](./Phase1-ImplementationGuide.md) | [Testing Guide](./Phase1-TestingGuide.md)

### Phase 2: Database Optimization (10-1,000 users)
- **Key Deliverables**: MongoDB integration, Redis caching, WebSocket prices
- **Technologies**: MongoDB, Redis, Spring Data, WebSocket
- [Implementation Guide](./Phase2-ImplementationGuide.md) | [Testing Guide](./Phase2-TestingGuide.md)

### Phase 3: Service Separation (100-1,000 users)
- **Key Deliverables**: API Gateway, NLP module, TradingView charts
- **Technologies**: Spring Cloud Gateway, sentiment analysis, lightweight-charts
- [Implementation Guide](./Phase3-ImplementationGuide.md) | [Testing Guide](./Phase3-TestingGuide.md)

### Phase 4: Async Processing (1,000-10,000 users)
- **Key Deliverables**: Kafka pipeline, async NLP, Redis Pub/Sub
- **Technologies**: Apache Kafka, Spring Kafka, event-driven architecture
- [Implementation Guide](./Phase4-ImplementationGuide.md) | [Testing Guide](./Phase4-TestingGuide.md)

### Phase 5: Microservices Preparation (10,000+ users)
- **Key Deliverables**: Docker images, OpenAPI docs, Kubernetes manifests
- **Technologies**: Docker, Kubernetes, OpenAPI/Swagger
- [Implementation Guide](./Phase5-ImplementationGuide.md) | [Testing Guide](./Phase5-TestingGuide.md)

---

## Command Reference

All guides include commands for both **Bash (Linux/macOS)** and **PowerShell (Windows 10/11)**.

### Quick Start Commands

**Bash (Linux/macOS):**
```bash
# Clone and setup
git clone https://github.com/HCMUS-Software-Architecture/Back-end.git
cd Back-end

# Start infrastructure
cd docker && docker compose up -d && cd ..

# Build and run
./mvnw clean package -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test
```

**PowerShell (Windows 10/11):**
```powershell
# Clone and setup
git clone https://github.com/HCMUS-Software-Architecture/Back-end.git
Set-Location Back-end

# Start infrastructure
Set-Location docker; docker compose up -d; Set-Location ..

# Build and run
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=dev"

# Run tests
.\mvnw.cmd test
```

---

## Related Documentation

- [Architecture.md](../Architecture.md) - System architecture overview
- [CoreRequirements.md](../CoreRequirements.md) - Business requirements
- [Features.md](../Features.md) - Feature specifications
- [ProjectPlan.md](../ProjectPlan.md) - Implementation timeline
- [Operations.md](../Operations.md) - Monitoring, CI/CD, Kubernetes
- [UseCaseDiagram.md](../UseCaseDiagram.md) - User interactions

---

## Troubleshooting

Common issues are documented in each phase guide. For quick reference:

| Issue | Phase | Solution Location |
|-------|-------|-------------------|
| PostgreSQL connection refused | 1, 2 | Phase1 Implementation Guide |
| MongoDB authentication failed | 2+ | Phase2 Implementation Guide |
| Kafka connection issues | 4+ | Phase4 Implementation Guide |
| WebSocket handshake failed | 2+ | Phase2/Phase3 Implementation Guide |
| Docker OOM killed | 5 | Phase5 Implementation Guide |
