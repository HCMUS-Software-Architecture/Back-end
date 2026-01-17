# Trading Platform - Backend

A full-stack trading platform with financial news aggregation, real-time price charts, and AI-powered analysis.

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Project Structure](#project-structure)
5. [Development Setup](#development-setup)
6. [Configuration](#configuration)
7. [Running the Application](#running-the-application)
8. [Documentation](#documentation)
9. [Technology Stack](#technology-stack)

---

## Overview

This platform provides:

- **Financial News Crawler**: Multi-source article collection with adaptive HTML parsing
- **Real-time Price Charts**: WebSocket-based TradingView-style charts
- **AI/NLP Analysis**: Sentiment analysis and trend prediction
- **Account Management**: User authentication and preferences

See [Architecture Overview](./docs/core/Architecture.md) for detailed design.

### Related Projects

- **Frontend**: [Front-end/README.md](../Front-end/README.md) - Next.js web application
- **Swagger API**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) - Interactive API docs

---

## Prerequisites

### Required Software

| Software | Version | Purpose                        |
| -------- | ------- | ------------------------------ |
| Java     | 17+     | Backend runtime                |
| Maven    | 3.8+    | Build tool                     |
| Node.js  | 18+     | Frontend runtime (for Next.js) |
| Git      | 2.30+   | Version control                |

### Recommended Tools

| Tool                    | Purpose             |
| ----------------------- | ------------------- |
| IntelliJ IDEA / VS Code | IDE                 |
| Postman / Insomnia      | API testing         |
| DBeaver                 | Database management |

### Verify Installation

These can run on both Windows and Linux-based OS

```powershell
# Check Java
java -version
# Expected: openjdk version "17.x.x" or higher

# Check Maven
mvn -version
# Expected: Apache Maven 3.8.x or higher

# Check Node.js (optional, for frontend)
node --version
# Expected: v18.x.x or higher
```

---

## Docker Compose Setup
Use this when actively working on the application:
```bash
docker compose up -d
```

## Quick Start Local

### 1. Clone the Repository

**Bash (Linux/macOS):**

```bash
git clone https://github.com/HCMUS-Software-Architecture/Back-end.git
cd Back-end
```

### 2. Set .env (If you are using Intellij as the main IDE)

Refer to this doc: https://stackoverflow.com/questions/71450194/how-do-i-add-environment-variables-in-intellij-spring-boot-project

**PowerShell (Windows 10/11):**

```powershell
git clone https://github.com/HCMUS-Software-Architecture/Back-end.git
Set-Location Back-end
```

### 3. Start Development Environment

**Bash (Linux/macOS):**

```bash
# Run the application with UTC timezone
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC"
```

**PowerShell (Windows 10/11):**

```powershell
# Run the application with UTC timezone
.\mvnw.cmd spring-boot:run -D"spring-boot.run.jvmArguments=-Duser.timezone=UTC"
```

### 4. Verify the Application

**Bash (Linux/macOS):**

```bash
# Health check
curl http://localhost:8081/actuator/health
```

**PowerShell (Windows 10/11):**

```powershell
# Health check
Invoke-RestMethod -Uri http://localhost:8081/actuator/health
```

> **Expected response:** `{"status":"UP"}`

---

## Project Structure

```
Back-end/
├── doc/                          # Documentation
│   ├── Architecture.md           # System architecture (evolutionary phases)
│   ├── CoreRequirements.md       # Business requirements
│   ├── Features.md               # Feature specifications
│   ├── ProjectPlan.md            # Implementation timeline
│   ├── UseCaseDiagram.md         # Use cases and flows
│   └── Operations.md             # Monitoring, CI/CD, Kubernetes guide
│
├── src/
│   ├── main/
│   │   ├── java/com/example/backend/
│   │   │   ├── BackEndApplication.java    # Main application entry
│   │   │   ├── config/                    # Configuration classes
│   │   │   ├── controller/                # REST controllers
│   │   │   ├── service/                   # Business logic
│   │   │   ├── repository/                # Data access
│   │   │   ├── model/                     # Domain entities
│   │   │   ├── dto/                       # Data transfer objects
│   │   │   └── crawler/                   # News crawler module
│   │   └── resources/
│   │       ├── application.yml            # Application configuration (git-ignored)
│   │       └── application.yml.example    # Configuration template (committed)
│   └── test/
│       └── java/com/example/backend/      # Test classes
│
├── docker/                        # Docker configurations (Phase 2+)
│   ├── docker-compose.yml         # Development stack
│   └── Dockerfile                 # Application container
│
├── .gitignore                     # Git ignore rules
├── pom.xml                        # Maven build configuration
├── mvnw                           # Maven wrapper (Unix)
├── mvnw.cmd                       # Maven wrapper (Windows)
└── README.md                      # This file
```

---

## API Documentation (Swagger/OpenAPI)

Once the application is running, access the interactive API documentation:

### Swagger UI

**URL:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

The Swagger UI provides:
- 📋 **Interactive API Explorer** - Test endpoints directly from the browser
- 📖 **Request/Response Schemas** - See all DTOs and data models
- 🔐 **Authentication** - Test endpoints with JWT tokens
- 📦 **Request Examples** - Sample payloads for all endpoints

### OpenAPI Specification

**JSON:** [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

Use the OpenAPI spec to:
- Generate client SDKs (TypeScript, Python, etc.)
- Import into Postman/Insomnia
- Validate API contracts

### API Endpoint Categories

| Category | Base Path | Description |
|----------|-----------|-------------|
| Health | `/api/health`, `/actuator/health` | Service health checks |
| Articles | `/api/articles` | News article CRUD operations |
| Prices | `/api/prices` | Price data and historical candles |
| Analysis | `/api/analysis` | NLP sentiment analysis |
| Auth | `/api/auth` | Authentication and authorization |

### WebSocket Endpoints

| Endpoint | Protocol | Description |
|----------|----------|-------------|
| `/ws/prices` | STOMP over SockJS | Real-time price updates |

**Subscribe to Topics:**
- `/topic/prices/{symbol}` - Price ticks for specific symbol
- `/topic/candles/{symbol}/{interval}` - Aggregated candles

---

## Documentation

| Document                                         | Description                                         |
| ------------------------------------------------ | --------------------------------------------------- |
| [Architecture.md](./docs/core/Architecture.md)   | Evolutionary architecture with phases and rationale |
| [CoreRequirements.md](./docs/core/CoreRequirements.md) | Business requirements                          |
| [DatabaseDesign.md](./docs/core/DatabaseDesign.md) | Database schemas and optimization                 |
| [UseCaseDiagram.md](./docs/core/UseCaseDiagram.md) | User interactions and flows                       |

### Implementation Guides

| Phase | Document | Focus |
|-------|----------|-------|
| 1 | [Phase1-ImplementationGuide.md](./docs/guides/Phase1-ImplementationGuide.md) | Monolithic foundation |
| 2 | [Phase2-ImplementationGuide.md](./docs/guides/Phase2-ImplementationGuide.md) | Database optimization |
| 3 | [Phase3-ImplementationGuide.md](./docs/guides/Phase3-ImplementationGuide.md) | Service separation |
| 4 | [Phase4-ImplementationGuide.md](./docs/guides/Phase4-ImplementationGuide.md) | Event-driven architecture |
| 5 | [Phase5-ImplementationGuide.md](./docs/guides/Phase5-ImplementationGuide.md) | Microservices ready |

---

## Technology Stack

### Backend

| Technology       | Purpose                 |
| ---------------- | ----------------------- |
| Java 17+         | Runtime                 |
| Spring Boot 3.x  | Application framework   |
| Spring Data JPA  | Database access         |
| Spring WebSocket | Real-time communication |
| Lombok           | Boilerplate reduction   |

### Databases (per phase)

| Phase | Databases                    |
| ----- | ---------------------------- |
| 1     | PostgreSQL (JSONB)           |
| 2+    | PostgreSQL + MongoDB + Redis |
| 4+    | + Apache Kafka               |
| 5+    | + TimescaleDB                |

### Frontend

| Technology         | Purpose             |
| ------------------ | ------------------- |
| Next.js            | React framework     |
| TradingView Charts | Price visualization |
| WebSocket          | Real-time updates   |

---

## Contributing

1. Create a feature branch from `develop`
2. Make changes following code style guidelines
3. Write tests for new functionality
4. Submit a pull request

---

## License

This project is for educational purposes.

---
