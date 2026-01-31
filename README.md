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

Có 2 cách chạy dự án:

---

### **🎯 Option 1: Docker Compose (Recommended cho Development)**

**Chạy tất cả services bằng Docker Compose**

**Ưu điểm:**
- ✅ Setup nhanh nhất (1 lệnh)
- ✅ Không cần cấu hình Kubernetes
- ✅ Phù hợp cho local development và testing
- ✅ Tự động service discovery qua Eureka

**Bước 1: Setup Environment**
```powershell
# Tạo file .env từ template
Copy-Item .env.example .env

# Chỉnh sửa .env với credentials của bạn
notepad .env
```

**Bước 2: Chạy Tất Cả Services**
```powershell
# Di chuyển vào thư mục back-end
cd Back-end

# Start tất cả services
docker compose up -d

# Kiểm tra trạng thái
docker compose ps

# Xem logs
docker compose logs -f

# Xem logs của một service cụ thể
docker compose logs -f price-service
docker compose logs -f api-gateway
```

**Bước 3: Verify Deployment**
```powershell
# Health check
Invoke-WebRequest http://localhost:8081/actuator/health

# Test price API
Invoke-WebRequest http://localhost:8081/api/prices/candles/BTCUSDT/1m?limit=5

# Truy cập Swagger UI
Start-Process "http://localhost:8081/swagger-ui.html"

# Eureka Dashboard
Start-Process "http://localhost:8761"
```

**Services đang chạy:**

| Service          | Port  | Container          | URL |
| ---------------- | ----- | ------------------ | --- |
| API Gateway      | 8081  | api-gateway        | http://localhost:8081 |
| Discovery Server | 8761  | discovery-server   | http://localhost:8761 |
| User Service     | 8082  | user-service       | http://localhost:8082 |
| Price Service    | 8083  | price-service      | http://localhost:8083 |
| Crawler Service  | 8084  | crawler-service    | http://localhost:8084 |
| News Service     | 8085  | news-service       | http://localhost:8085 |
| Analysis Service | 8000  | analysis-service   | http://localhost:8000 |
| MongoDB          | 27017 | trading-mongodb    | mongodb://localhost:27017 |
| Redis            | 6379  | trading-redis      | redis://localhost:6379 |
| RabbitMQ         | 5672  | trading-rabbitmq   | amqp://localhost:5672 |
| RabbitMQ UI      | 15672 | trading-rabbitmq   | http://localhost:15672 |

**Stop Services:**
```powershell
# Stop tất cả
docker compose down

# Stop và xóa volumes (reset database)
docker compose down -v

# Rebuild và restart
docker compose up -d --build
```

---

### **🚀 Option 2: Hybrid (Docker Compose + Kubernetes)**

**Docker Compose cho infrastructure + Kubernetes cho Price Service với HPA**

**Ưu điểm:**
- ✅ Test autoscaling thực tế (Kubernetes HPA)
- ✅ Production-like environment
- ✅ Infrastructure management dễ dàng (Docker)
- ✅ Price service có thể scale 1-3 replicas tự động

**Use case:** Khi cần test load balancing và autoscaling của price service

**Architecture:**
```
Docker Compose:  mongodb, redis, rabbitmq, discovery-server, api-gateway, 
                 user-service, crawler-service, news-service, analysis-service
                 
Kubernetes:      price-service (1-3 replicas @ 70% CPU, NodePort 30083)
```

**Bước 1: Setup Environment**
```powershell
# Tạo .env file
Copy-Item .env.example .env
notepad .env

# Thêm config cho hybrid mode vào .env:
PRICE_SERVICE_URI=http://host.docker.internal:30083
PRICE_SERVICE_WS_URI=ws://host.docker.internal:30083
```

**Bước 2: Start Infrastructure & Backend Services (Docker)**
```powershell
cd Back-end

# Start tất cả TRỪ price-service
docker compose up -d mongodb redis rabbitmq discovery-server api-gateway user-service crawler-service news-service analysis-service

# Verify Docker services đang chạy
docker compose ps
# Expected: 9 containers running (không có price-service)
```

**Bước 3: Deploy Price Service to Kubernetes**
```powershell
# Build price-service image
docker compose build price-service

# Create Kubernetes secrets (first time only)
powershell -ExecutionPolicy Bypass -File scripts/create-k8s-secrets.ps1

# Create namespace
kubectl create namespace trading-system

# Deploy price-service
kubectl apply -f k8s/deployments/price-service-deployment.yaml
kubectl apply -f k8s/services/price-service-service.yaml
kubectl apply -f k8s/autoscaling/price-service-hpa.yaml

# Verify Kubernetes deployment
kubectl get all -n trading-system

# Wait for pod to be Ready (1/1)
kubectl get pods -n trading-system -w
```

**Bước 4: Configure API Gateway để kết nối với K8s Price Service**
```powershell
# Đảm bảo .env có 2 dòng này:
# PRICE_SERVICE_URI=http://host.docker.internal:30083
# PRICE_SERVICE_WS_URI=ws://host.docker.internal:30083

# Restart API Gateway để load config mới
docker compose restart api-gateway

# Kiểm tra logs
docker compose logs -f api-gateway
```

**Bước 5: Verify Hybrid Setup**
```powershell
# 1. Check Docker services (9 containers)
docker compose ps

# 2. Check Kubernetes (1 price-service pod)
kubectl get pods -n trading-system
kubectl get hpa -n trading-system

# 3. Test price-service health từ bên trong pod
kubectl exec -n trading-system -it $(kubectl get pod -n trading-system -l app=price-service -o jsonpath='{.items[0].metadata.name}') -- curl -s http://localhost:8083/actuator/health
# Expected: {"status":"UP"}

# 4. Test qua API Gateway (end-to-end)
Invoke-WebRequest http://localhost:8081/api/prices/candles/BTCUSDT/1m?limit=5
# Expected: HTTP 200 với candle data

# 5. Test WebSocket connection
# Mở browser console tại http://localhost:3000 (Frontend)
# Check WebSocket connection status

# 6. Monitor HPA autoscaling
kubectl get hpa -n trading-system -w
# NAME            REFERENCE                  TARGETS   MINPODS   MAXPODS   REPLICAS
# price-service   Deployment/price-service   20%/70%   1         3         1
```

**Kiểm Tra Network Connectivity:**
```powershell
# Từ API Gateway container → K8s Price Service
docker exec api-gateway curl http://host.docker.internal:30083/actuator/health

# Từ K8s pod → Docker MongoDB (nếu cần)
kubectl exec -n trading-system -it $(kubectl get pod -n trading-system -l app=price-service -o jsonpath='{.items[0].metadata.name}') -- curl -s http://host.docker.internal:27017
```

**Monitor Kubernetes:**
```powershell
# Watch pods scale up/down
kubectl get pods -n trading-system -w

# View HPA metrics
kubectl describe hpa price-service -n trading-system

# Check resource usage
kubectl top pods -n trading-system

# View logs
kubectl logs -n trading-system -l app=price-service -f --tail=50
```

**Cleanup/Revert to Docker-Only:**
```powershell
# Stop Kubernetes price-service
kubectl delete namespace trading-system

# Xóa hybrid config trong .env (comment out hoặc xóa):
# PRICE_SERVICE_URI=http://host.docker.internal:30083
# PRICE_SERVICE_WS_URI=ws://host.docker.internal:30083

# Restart API Gateway
docker compose restart api-gateway

# Start price-service trong Docker
docker compose up -d price-service

# Verify
docker compose ps
# Expected: 10 containers including price-service
```

---

### **🔧 Network Configuration Explained**

**Cách API Gateway (Docker) giao tiếp với Price Service (Kubernetes):**

1. **Price Service** chạy trong Kubernetes với **NodePort 30083**
   - Pod internal port: `8083`
   - Service type: `NodePort`
   - NodePort: `30083` (exposed trên host machine)

2. **API Gateway** chạy trong Docker container
   - Sử dụng `host.docker.internal` để truy cập host machine
   - `host.docker.internal:30083` → K8s NodePort → Price Service Pod

3. **Environment Variables** trong API Gateway (.env):
   ```env
   # Khi không set → dùng Eureka service discovery (Docker mode)
   # Khi set → override và gọi trực tiếp đến K8s (Hybrid mode)
   PRICE_SERVICE_URI=http://host.docker.internal:30083
   PRICE_SERVICE_WS_URI=ws://host.docker.internal:30083
   ```

4. **Luồng request:**
   ```
   Frontend (localhost:3000) 
     → API Gateway (localhost:8081)
     → host.docker.internal:30083 (Kubernetes NodePort)
     → Price Service Pod (8083)
   ```

**Troubleshooting Network Issues:**

| Vấn đề | Nguyên nhân | Giải pháp |
|--------|-------------|-----------|
| `Connection refused` | K8s pod chưa ready | `kubectl get pods -n trading-system` - đợi 1/1 Ready |
| `404 Not Found` | NodePort sai | Verify `kubectl get svc -n trading-system` |
| `host.docker.internal not found` | Docker Desktop config | Enable "Use Kubernetes" trong Docker Desktop |
| API Gateway không kết nối được | Env vars chưa set | Kiểm tra `.env` và restart api-gateway |
| WebSocket disconnect | Port mapping sai | Verify `PRICE_SERVICE_WS_URI` |

---

### **📊 So Sánh 2 Options**

| Feature | Docker Compose Only | Hybrid (Docker + K8s) |
|---------|---------------------|----------------------|
| **Setup Time** | ⚡ 2 phút | 🕐 5-10 phút |
| **Complexity** | 🟢 Đơn giản | 🟡 Trung bình |
| **Autoscaling** | ❌ Không có | ✅ HPA (1-3 replicas) |
| **Resource Usage** | 🟢 Thấp | 🟡 Cao hơn |
| **Production-like** | 🟡 Cơ bản | 🟢 Giống production |
| **Debugging** | 🟢 Dễ | 🟡 Phức tạp hơn |
| **Use Case** | Local dev, testing | Load testing, demo autoscaling |

**Khuyến nghị:**
- **Development daily:** Dùng **Option 1** (Docker Compose only)
- **Demo autoscaling:** Dùng **Option 2** (Hybrid)
- **Production:** Deploy tất cả services lên Kubernetes với HPA

---

### ☸️ Kubernetes Monitoring & Management

### ☸️ Kubernetes Monitoring & Management

**Sử dụng khi chạy Option 2 (Hybrid mode):**

```powershell
# Watch pods scaling (realtime)
kubectl get pods -n trading-system -w

# View HPA status và metrics
kubectl get hpa -n trading-system
kubectl describe hpa price-service -n trading-system

# Check resource usage (CPU, Memory)
kubectl top pods -n trading-system
kubectl top nodes

# View logs
kubectl logs -n trading-system -l app=price-service -f --tail=100

# Stream logs từ tất cả replicas
kubectl logs -n trading-system -l app=price-service -f --prefix=true

# Exec vào pod để debug
kubectl exec -n trading-system -it $(kubectl get pod -n trading-system -l app=price-service -o jsonpath='{.items[0].metadata.name}') -- /bin/sh

# Port forward để test trực tiếp
kubectl port-forward -n trading-system svc/price-service 8083:8083

# Restart pod
kubectl rollout restart deployment/price-service -n trading-system

# Scale manually (override HPA temporarily)
kubectl scale deployment price-service -n trading-system --replicas=3

# View events
kubectl get events -n trading-system --sort-by='.lastTimestamp'
```

**Cleanup Kubernetes:**
```powershell
# Delete tất cả resources trong namespace
kubectl delete namespace trading-system

# Hoặc delete từng resource
kubectl delete -f k8s/autoscaling/price-service-hpa.yaml
kubectl delete -f k8s/services/price-service-service.yaml
kubectl delete -f k8s/deployments/price-service-deployment.yaml
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
# Health check
Invoke-WebRequest http://localhost:8081/actuator/health

# Price data
Invoke-WebRequest http://localhost:8081/api/prices/candles/BTCUSDT/1m?limit=5
```

---

## 🏃 Quick Start - Chạy Dự Án

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
