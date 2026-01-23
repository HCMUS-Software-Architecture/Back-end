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

| Software       | Version | Purpose                        |
| -------------- | ------- | ------------------------------ |
| Java           | 21      | Backend runtime                |
| Maven          | 3.8+    | Build tool                     |
| Docker         | 20.10+  | Containerization               |
| Docker Compose | 2.0+    | Multi-container orchestration  |
| Node.js        | 18+     | Frontend runtime (for Next.js) |
| Git            | 2.30+   | Version control                |

### Required Environment Files

Before running the application, you need to create the following configuration files:

1. **micro.env** - Main environment configuration (copy from `micro.env.example`)
2. **src/main/resources/application.yml** - Application configuration (copy from `application.yml.example`)

```powershell
# Create micro.env from example
Copy-Item micro.env.example micro.env

# Create application.yml from example
Copy-Item src\main\resources\application.yml.example src\main\resources\application.yml

# Edit these files with your actual credentials
```

### Recommended Tools

| Tool                    | Purpose             |
| ----------------------- | ------------------- |
| IntelliJ IDEA / VS Code | IDE                 |
| Postman / Insomnia      | API testing         |
| DBeaver                 | Database management |

### Verify Installation

```powershell
# Check Java (must be version 21)
java -version
# Expected: openjdk version "21.x.x"

# Check Maven
mvn -version
# Expected: Apache Maven 3.8.x or higher

# Check Docker
docker --version
# Expected: Docker version 20.10.x or higher

# Check Docker Compose
docker compose version
# Expected: Docker Compose version v2.x.x or higher

# Check Node.js (optional, for frontend)
node --version
# Expected: v18.x.x or higher
```

---

## Docker Compose Setup (Recommended)

Use Docker Compose for running the application with all required services (PostgreSQL, MongoDB, Redis, RabbitMQ):

```powershell
# Start all services in detached mode
docker compose up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Rebuild and restart services
docker compose down
docker compose build --no-cache
docker compose up -d
```

### Docker Services

The following services will be started:

| Service     | Port  | Purpose                      |
| ----------- | ----- | ---------------------------- |
| PostgreSQL  | 5432  | Relational database          |
| MongoDB     | 27017 | Document database (articles) |
| Redis       | 6379  | Cache & session storage      |
| RabbitMQ    | 5672  | Message broker (STOMP)       |
| RabbitMQ UI | 15672 | RabbitMQ management console  |

---

## Quick Start Local

### 1. Clone the Repository

```powershell
git clone https://github.com/HCMUS-Software-Architecture/Back-end.git
Set-Location Back-end
```

### 2. Set Up Environment Files

```powershell
# Create micro.env from example
Copy-Item micro.env.example micro.env

# Create application.yml from example
Copy-Item src\main\resources\application.yml.example src\main\resources\application.yml

# Edit the files with your actual credentials
# For IntelliJ: https://stackoverflow.com/questions/71450194/how-do-i-add-environment-variables-in-intellij-spring-boot-project
notepad micro.env
notepad src\main\resources\application.yml
```

### 3. Start Docker Services

```powershell
# Start all required services (PostgreSQL, MongoDB, Redis, RabbitMQ)
docker compose up -d

# Verify all services are running
docker compose ps
```

### 4. Run the Application

```powershell
# Build and run with Maven
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run -D"spring-boot.run.jvmArguments=-Duser.timezone=UTC"
```

### 5. Run Tests

```powershell
# Run all unit tests
.\mvnw.cmd test

# Run unit tests only (skip integration tests that need full context)
.\mvnw.cmd test -Dtest=*Test -DfailIfNoTests=false

# Run specific test class
.\mvnw.cmd test -Dtest=JwtServiceTest

# Run with coverage report
.\mvnw.cmd clean verify
```

### 6. Verify the Application

```powershell
# Health check
Invoke-RestMethod -Uri http://localhost:8081/actuator/health

# Access Swagger UI
Start-Process "http://localhost:8081/swagger-ui.html"
```

> **Expected response:** `{"status":"UP"}`

---

## Project Structure

```
Back-end/
├── docs/                          # Documentation
│   ├── core/                      # Core architecture docs
│   │   ├── Architecture.md        # System architecture (evolutionary phases)
│   │   ├── CoreRequirements.md    # Business requirements
│   │   ├── DatabaseDesign.md      # Database schemas and optimization
│   │   ├── Features.md            # Feature specifications
│   │   ├── Operations.md          # Monitoring, CI/CD, Kubernetes guide
│   │   ├── UIUXGuidelines.md      # UI/UX design guidelines
│   │   └── UseCaseDiagram.md      # Use cases and flows
│   ├── guides/                    # Implementation guides
│   │   ├── Phase2-ImplementationGuide.md
│   │   ├── Phase3-ImplementationGuide.md
│   │   ├── Phase4-ImplementationGuide.md
│   │   ├── Phase5-ImplementationGuide.md
│   │   └── Testing guides...
│   ├── api/                       # API documentation
│   │   ├── README.md
│   │   └── Websocket.md
│   ├── ProjectPlan.md             # Implementation timeline
│   ├── Price-Collector-Architecture.md
│   └── TESTING_SUMMARY.md
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
│   │   │   ├── exception/                 # Custom exceptions
│   │   │   ├── security/                  # Security & JWT
│   │   │   └── crawler/                   # News crawler module
│   │   └── resources/
│   │       ├── application.yml            # Application config (git-ignored)
│   │       └── application.yml.example    # Config template (committed)
│   └── test/
│       └── java/com/example/backend/      # Test classes
│           ├── service/                   # Unit tests
│           └── integration/               # Integration tests
│
├── api-gateway/                   # API Gateway microservice
│   ├── src/
│   └── pom.xml
│
├── discovery-server/              # Service discovery (Eureka)
│   ├── src/
│   └── pom.xml
│
├── price-service/                 # Price data microservice
│   ├── src/
│   └── pom.xml
│
├── user-service/                  # User management microservice
│   ├── src/
│   └── pom.xml
│
├── nginx/                         # NGINX configuration
│   └── nginx.conf
│
├── target/                        # Build output (git-ignored)
│
├── .gitignore                     # Git ignore rules
├── docker-compose.yml             # Docker services configuration
├── Dockerfile                     # Application container
├── micro.env.example              # Environment variables template
├── micro.env                      # Actual env variables (git-ignored)
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

| Category | Base Path                         | Description                       |
| -------- | --------------------------------- | --------------------------------- |
| Health   | `/api/health`, `/actuator/health` | Service health checks             |
| Articles | `/api/articles`                   | News article CRUD operations      |
| Prices   | `/api/prices`                     | Price data and historical candles |
| Analysis | `/api/analysis`                   | NLP sentiment analysis            |
| Auth     | `/api/auth`                       | Authentication and authorization  |

### WebSocket Endpoints

| Endpoint     | Protocol          | Description             |
| ------------ | ----------------- | ----------------------- |
| `/ws/prices` | STOMP over SockJS | Real-time price updates |

**Subscribe to Topics:**

- `/topic/prices/{symbol}` - Price ticks for specific symbol
- `/topic/candles/{symbol}/{interval}` - Aggregated candles

---

## Documentation

| Document                                               | Description                                         |
| ------------------------------------------------------ | --------------------------------------------------- |
| [Architecture.md](./docs/core/Architecture.md)         | Evolutionary architecture with phases and rationale |
| [CoreRequirements.md](./docs/core/CoreRequirements.md) | Business requirements                               |
| [DatabaseDesign.md](./docs/core/DatabaseDesign.md)     | Database schemas and optimization                   |
| [UseCaseDiagram.md](./docs/core/UseCaseDiagram.md)     | User interactions and flows                         |

### Implementation Guides

| Phase | Document                                                                     | Focus                     |
| ----- | ---------------------------------------------------------------------------- | ------------------------- |
| 1     | [Phase1-ImplementationGuide.md](./docs/guides/Phase1-ImplementationGuide.md) | Monolithic foundation     |
| 2     | [Phase2-ImplementationGuide.md](./docs/guides/Phase2-ImplementationGuide.md) | Database optimization     |
| 3     | [Phase3-ImplementationGuide.md](./docs/guides/Phase3-ImplementationGuide.md) | Service separation        |
| 4     | [Phase4-ImplementationGuide.md](./docs/guides/Phase4-ImplementationGuide.md) | Event-driven architecture |
| 5     | [Phase5-ImplementationGuide.md](./docs/guides/Phase5-ImplementationGuide.md) | Microservices ready       |

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
