# Trading Platform - Backend

A full-stack trading platform with financial news aggregation, real-time price charts, and AI-powered analysis.

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Deployment Options](#deployment-options)
   - [Docker Compose (Development)](#docker-compose-development)
   - [Kubernetes (Production)](#kubernetes-production)
5. [Project Structure](#project-structure)
6. [Development Setup](#development-setup)
7. [Configuration](#configuration)
8. [Running the Application](#running-the-application)
9. [Documentation](#documentation)
10. [Technology Stack](#technology-stack)

---

## Overview

This platform provides:

- **Financial News Crawler**: Multi-source article collection with adaptive HTML parsing
- **Real-time Price Charts**: WebSocket-based TradingView-style charts
- **AI/NLP Analysis**: Sentiment analysis and trend prediction
- **Account Management**: User authentication and preferences with Google OAuth

See [Architecture Overview](./docs/core/Architecture.md) for detailed design.

### Architecture Overview

```
┌──────────────┐
│   Frontend   │  Next.js (Port 3000)
│  (Next.js)   │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│                      API Gateway                              │
│                      Port: 8081                               │
│  • JWT Authentication  • Request Routing  • CORS              │
└───────┬──────────────────────────────────────────────────────┘
        │
        ├─────────────────┬──────────────────┬──────────────────┐
        ▼                 ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│    Eureka    │  │User Service  │  │Price Service │  │Price Collector│
│  Discovery   │  │  Port: 8082  │  │  Port: 8083  │  │  Port: 8084  │
│ Port: 8761   │  │              │  │              │  │              │
│              │  │• Auth/OAuth  │  │• Price API   │  │• Binance API │
│              │  │• JWT Tokens  │  │• WebSocket   │  │• Price Feeds │
└──────────────┘  │• User CRUD   │  │• Candles     │  └──────────────┘
                  └──────┬───────┘  └──────┬───────┘
                         │                 │
                         ▼                 ▼
                  ┌──────────────┐  ┌──────────────┐
                  │   MongoDB    │  │  PostgreSQL  │
                  │  Port: 27017 │  │  Port: 5432  │
                  │              │  │              │
                  │• users       │  │• price data  │
                  │• tokens      │  │• candles     │
                  └──────────────┘  └──────────────┘
```

### Service Ports

| Service          | Port  | Purpose                         |
| ---------------- | ----- | ------------------------------- |
| Frontend         | 3000  | Next.js web application         |
| API Gateway      | 8081  | Entry point, routing, JWT auth  |
| Discovery Server | 8761  | Eureka service registry         |
| User Service     | 8082  | Authentication, user management |
| Price Service    | 8083  | Price data, WebSocket, candles  |
| Price Collector  | 8084  | Binance API integration         |
| PostgreSQL       | 5432  | Price and historical data       |
| MongoDB          | 27017 | User data and documents         |
| Redis            | 6379  | Cache and session storage       |
| RabbitMQ         | 5672  | Message broker (STOMP)          |
| RabbitMQ UI      | 15672 | Management console              |

### Related Projects

- **Frontend**: [Front-end/README.md](../Front-end/README.md) - Next.js web application
- **Swagger API**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) - Interactive API docs

---

## Prerequisites

### Required Software

| Software       | Version | Purpose                                     |
| -------------- | ------- | ------------------------------------------- |
| Java           | 21      | Backend runtime                             |
| Maven          | 3.8+    | Build tool                                  |
| Docker Desktop | 20.10+  | Containerization + Kubernetes               |
| Docker Compose | 2.0+    | Multi-container orchestration               |
| kubectl        | 1.28+   | Kubernetes CLI (included in Docker Desktop) |
| Node.js        | 18+     | Frontend runtime (for Next.js)              |
| Git            | 2.30+   | Version control                             |

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

# Check kubectl (Kubernetes CLI)
kubectl version --client
# Expected: Client Version: v1.28.x or higher
```

---

## Deployment Options

### 🚀 Hybrid Approach (Recommended)

**Infrastructure (Docker Compose) + Application Services (Kubernetes)**

This approach gives you:

- ✅ Easy database management with Docker Compose
- ✅ Production-ready autoscaling with Kubernetes
- ✅ Realistic production environment locally

**Quick Start:**

```powershell
# 1. Start infrastructure
docker compose up -d postgres mongodb redis rabbitmq

# 2. Build images
docker compose build discovery-server api-gateway user-service price-service analysis-service

# 3. Deploy to Kubernetes
kubectl apply -f k8s/namespace.yaml
.\scripts\create-k8s-secrets.ps1
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/
kubectl apply -f k8s/autoscaling/

# 4. Port-forward services (after verifying pods are ready: kubectl get pods -n trading-system)
kubectl port-forward -n trading-system svc/price-service 8083:8083
kubectl port-forward -n trading-system svc/analysis-service 8000:8000
```

> **⚠️ Important**: Wait 1-2 minutes after step 3 for pods to become ready. Check with `kubectl get pods -n trading-system` - wait for READY column to show `1/1`.

📖 **[Full Hybrid Deployment Guide](./docs/HYBRID_DEPLOYMENT_GUIDE.md)**  
📖 **[Troubleshooting Guide](#-troubleshooting-kubernetes-deployment)**

---

### 🐳 Docker Compose Only

**All services in Docker Compose (fastest setup)**

**Best for**: Quick testing, no Kubernetes needed.

```powershell
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Stop all
docker compose down
```

**Services:**

| Service          | Port  | Container          |
| ---------------- | ----- | ------------------ |
| API Gateway      | 8081  | api-gateway        |
| Discovery Server | 8761  | discovery-server   |
| User Service     | 8082  | user-service       |
| Price Service    | 8083  | price-service (2x) |
| Analysis Service | 8000  | analysis-service   |
| PostgreSQL       | 5432  | trading-postgres   |
| MongoDB          | 27017 | trading-mongodb    |
| Redis            | 6379  | trading-redis      |
| RabbitMQ         | 5672  | trading-rabbitmq   |
| RabbitMQ UI      | 15672 | trading-rabbitmq   |

---

### ☸️ Kubernetes Monitoring & Management

Once deployed with hybrid or K8s approach:

```powershell
# Watch pods scale
kubectl get pods -n trading-system -w

# View HPA status
kubectl get hpa -n trading-system

# Check resource usage
kubectl top pods -n trading-system

# View logs
kubectl logs -n trading-system -l app=price-service -f
kubectl logs -n trading-system -l app=analysis-service -f

# Stop deployment
kubectl delete namespace trading-system
docker compose down
docker compose build --no-cache
docker compose up -d
```

### Docker Services

The following services will be started:

| Service                  | Port  | Purpose                            |
| ------------------------ | ----- | ---------------------------------- |
| **Infrastructure**       |       |                                    |
| PostgreSQL               | 5432  | Relational database (prices)       |
| MongoDB                  | 27017 | Document database (articles/users) |
| Redis                    | 6379  | Cache & session storage            |
| RabbitMQ                 | 5672  | Message broker (AMQP)              |
| RabbitMQ UI              | 15672 | RabbitMQ management console        |
| RabbitMQ STOMP           | 3001  | STOMP WebSocket relay              |
| **Spring Boot Services** |       |                                    |
| Discovery Server         | 8761  | Eureka service registry            |
| API Gateway              | 8081  | Single entry point, routing        |
| User Service             | 8082  | Authentication, subscriptions      |
| Price Service            | 8083  | Price API (multiple replicas)      |
| Price Collector          | 8086  | Binance WebSocket collector        |
| **NestJS Services**      |       |                                    |
| News Service             | 8085  | Article retrieval API              |
| Crawler Service          | 8084  | Multi-source news crawler          |
| **Frontend**             |       |                                    |
| Next.js Frontend         | 3000  | React-based trading UI             |

### Full Stack Docker Deployment

```powershell
# Build and start all services (first time may take 10-15 minutes)
docker compose up -d --build

# Watch the logs
docker compose logs -f

# Check service health
docker compose ps

# Access the application
# - Frontend: http://localhost:3000
# - API Gateway: http://localhost:8081
# - Eureka Dashboard: http://localhost:8761
# - RabbitMQ UI: http://localhost:15672 (guest/guest)
# - API Documentation: http://localhost:8081/swagger-ui.html
```

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
