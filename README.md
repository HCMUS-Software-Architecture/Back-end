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

### 🚀 Hybrid Approach (Recommended for Production-Like Testing)

**Infrastructure + Most Services (Docker Compose) + Price Service Only (Kubernetes with HPA)**

This approach gives you:

- ✅ Easy database management with Docker Compose
- ✅ Production-ready autoscaling for price-service with Kubernetes HPA
- ✅ Realistic production environment locally
- ✅ Backwards compatible - can run full Docker Compose standalone

**Architecture:**

```
Docker Compose:  postgres, mongodb, redis, rabbitmq, discovery-server, api-gateway, user-service, analysis-service
Kubernetes:      price-service (1-3 replicas @ 70% CPU, NodePort 30083)
```

**Quick Start:**

```powershell
# 1. Start infrastructure + backend services in Docker Compose
cd Back-end
docker compose up -d --build postgres mongodb redis rabbitmq discovery-server api-gateway user-service analysis-service

# 2. Build price-service image
docker compose build price-service

# 3. Create Kubernetes secrets (REQUIRED - first time only)
powershell -ExecutionPolicy Bypass -File scripts/create-k8s-secrets.ps1

# 4. Deploy price-service to Kubernetes
kubectl create namespace trading-system
kubectl apply -f k8s/deployments/price-service-deployment.yaml
kubectl apply -f k8s/services/price-service-service.yaml
kubectl apply -f k8s/autoscaling/price-service-hpa.yaml

# 4. Configure API Gateway for hybrid mode (add to your .env file)
# Add these lines to .env:
PRICE_SERVICE_URI=http://host.docker.internal:30083
PRICE_SERVICE_WS_URI=ws://host.docker.internal:30083

# 5. Restart API Gateway to pick up hybrid configuration
docker compose restart api-gateway

# 6. Verify deployment
kubectl get pods -n trading-system       # Wait for price-service pod to be 1/1 Running
kubectl get hpa -n trading-system        # Check HPA status (should show <70% CPU)
kubectl exec -n trading-system -it $(kubectl get pod -n trading-system -l app=price-service -o jsonpath='{.items[0].metadata.name}') -- curl -s http://localhost:8083/actuator/health  # Test from inside pod
```

**Verify Hybrid Setup:**

```powershell
# Check Docker services (should see 8 containers)
docker compose ps

# Check K8s (should see 1 price-service pod)
kubectl get all -n trading-system

# Test health endpoint from inside the pod (NodePort may not work on Windows/Docker Desktop)
kubectl exec -n trading-system -it $(kubectl get pod -n trading-system -l app=price-service -o jsonpath='{.items[0].metadata.name}') -- curl -s http://localhost:8083/actuator/health
# Expected: {"groups":["liveness","readiness"],"status":"UP"}

# Test API endpoint through API Gateway (this works because Docker can reach K8s NodePort)
Invoke-WebRequest http://localhost:8081/api/prices/candles/BTCUSDT/1m?limit=5
```

**To Revert to Docker-Only Mode:**

```powershell
# Remove hybrid environment variables from .env:
# PRICE_SERVICE_URI=...
# PRICE_SERVICE_WS_URI=...

# Delete K8s resources
kubectl delete namespace trading-system

# Start full Docker Compose stack
docker compose up -d
```

> **⚠️ Important**: The API Gateway automatically uses Eureka service discovery (Docker Compose mode) unless you explicitly set `PRICE_SERVICE_URI` and `PRICE_SERVICE_WS_URI` environment variables for hybrid mode.

📖 **[CPU Metrics & HPA Explained](./docs/todo8.md#2-cpu-metrics-explained)**  
📖 **[Hybrid Architecture Details](./docs/todo8.md#1-architecture-issues--solutions)**  
📖 **[Troubleshooting Guide](#-troubleshooting-kubernetes-deployment)**

---

### 🐳 Docker Compose Only (Fastest for Development)

**All services in Docker Compose (no Kubernetes needed)**

**Best for**: Quick testing, development, no autoscaling requirements.

```powershell
# Start all services
docker compose up -d

# View logs
docker compose logs -f price-service

# Stop all
docker compose down
```

**Services:**

| Service          | Port  | Replicas      | Container                        |
| ---------------- | ----- | ------------- | -------------------------------- |
| API Gateway      | 8081  | 1             | api-gateway                      |
| Discovery Server | 8761  | 1             | discovery-server                 |
| User Service     | 8082  | 1             | user-service                     |
| Price Service    | 8083  | 2 (load-bal.) | price-service-1, price-service-2 |
| Price Collector  | 8084  | 1             | price-collector                  |
| Analysis Service | 8000  | 1             | analysis-service                 |
| PostgreSQL       | 5432  | 1             | trading-postgres                 |
| MongoDB          | 27017 | 1             | trading-mongodb                  |
| Redis            | 6379  | 1             | trading-redis                    |
| RabbitMQ         | 5672  | 1             | trading-rabbitmq                 |
| RabbitMQ UI      | 15672 | 1             | trading-rabbitmq                 |

**Test API:**

```powershell
# Health check
Invoke-WebRequest http://localhost:8081/actuator/health

# Price data
Invoke-WebRequest http://localhost:8081/api/prices/candles/BTCUSDT/1m?limit=5
```

---

### ☸️ Kubernetes Monitoring & Management

Once deployed with hybrid approach:

```powershell
# Watch pods scale (HPA adjusts replicas based on CPU%)
kubectl get pods -n trading-system -w

# View HPA status
kubectl get hpa -n trading-system
# Expected output: cpu: 11%/70%, REPLICAS: 1-3

# Check resource usage
kubectl top pods -n trading-system

# View logs
kubectl logs -n trading-system -l app=price-service --tail=50 -f

# Describe pod for troubleshooting
kubectl describe pod -n trading-system -l app=price-service

# Stop deployment
kubectl delete namespace trading-system
docker compose down
```

**Understanding HPA CPU Metrics:**

⚠️ **HPA monitors POD CPU%, NOT your laptop/host CPU%**

```yaml
# Example: price-service requests 100m CPU
resources:
  requests:
    cpu: 100m  # 0.1 CPU cores
  limits:
    cpu: 500m  # 0.5 CPU cores max

# HPA Calculation:
cpu% = (pod actual CPU usage / CPU request) × 100

# If pod uses 50 millicores:
#   50m / 100m = 50% CPU
# HPA will scale UP when avg > 70% (all pods combined)
# HPA will scale DOWN when avg < 70% for 5 minutes
```

**Common HPA Scenarios:**

- `cpu: 11%/70%, REPLICAS: 1` → **Normal**: Low load, 1 pod sufficient
- `cpu: 85%/70%, REPLICAS: 2` → **Scaling up**: High load, adding pods
- `cpu: 45%/70%, REPLICAS: 3` → **Scaling down soon**: Load decreased, will remove pod after 5min

**Why did I see 3 pods when I only opened 1 browser tab?**  
→ Pods were crashing (CrashLoopBackOff), not CPU-based scaling. K8s tried to maintain `minReplicas: 1` by spawning new pods. See [docs/todo8.md](./docs/todo8.md#22-why-did-price-service-scale-to-3-pods-earlier) for full explanation.

📖 **[Kubernetes Autoscaling Documentation](./docs/KUBERNETES_DEPLOYMENT_SUMMARY.md)**  
📖 **[CPU Metrics Deep Dive](./docs/todo8.md#2-cpu-metrics-explained)**

---

### � Troubleshooting Kubernetes Deployment

Common issues and solutions when deploying to Kubernetes:

#### **Pods in ImagePullBackOff**

**Problem**: Kubernetes can't find the Docker images

```powershell
# Check images exist
docker images | Select-String "trading-application"
```

**Solution**: Ensure `imagePullPolicy: Never` in deployments and images built with correct names:

```powershell
# Build images with Docker Compose (creates trading-application-* prefix)
docker compose build discovery-server api-gateway user-service price-service analysis-service
```

#### **Pods in CrashLoopBackOff or CreateContainerConfigError**

**Problem**: Application fails to start or can't find secrets

```powershell
# Check pod logs
kubectl logs -n trading-system -l app=price-service --tail=50
kubectl logs -n trading-system -l app=analysis-service --tail=50

# Check if secret exists
kubectl get secret trading-secrets -n trading-system
```

**Common causes:**

1. **Secret not found**: Missing `trading-secrets` - run the create script first
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/create-k8s-secrets.ps1
   ```
2. **MongoDB connection error**: Check `MONGODB_URI` env var is correct (not `SPRING_DATA_MONGODB_URI`)
3. **Missing environment variables**: Verify secret exists and is mounted correctly
4. **Infrastructure not running**: Start Docker Compose services first

```powershell
# Verify secret
kubectl get secret trading-secrets -n trading-system -o yaml

# Check decoded values
kubectl get secret trading-secrets -n trading-system -o jsonpath='{.data.MONGODB_URI}' | %{[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_))}

# After fixing secrets, delete pod to force recreation
kubectl delete pod -n trading-system -l app=price-service
```

#### **Health Check Failures**

**Problem**: Readiness probe returns 404

**Solutions:**

- **Price Service**: Ensure `/actuator/health` endpoint exists (Spring Boot Actuator dependency)
- **Analysis Service**: Ensure `/health` endpoint implemented in FastAPI

#### **Port-Forward Fails or NodePort Not Working**

**Problem**: Can't access services via NodePort on Windows/Docker Desktop

**NodePort Limitation**: Docker Desktop on Windows has networking limitations with NodePort. Services are healthy but NodePort (30083) may not be accessible from host.

**Solutions:**

1. **Use Port-Forward (Recommended for local testing)**:

   ```powershell
   # Forward service port to localhost
   kubectl port-forward -n trading-system svc/price-service 8083:8083

   # In another terminal, test the endpoint
   curl http://localhost:8083/actuator/health
   ```

2. **Test from inside the pod** (verify service is actually running):

   ```powershell
   kubectl exec -n trading-system -it $(kubectl get pod -n trading-system -l app=price-service -o jsonpath='{.items[0].metadata.name}') -- curl -s http://localhost:8083/actuator/health
   # Expected: {"groups":["liveness","readiness"],"status":"UP"}
   ```

3. **For API Gateway to access K8s services**: The hybrid setup uses `host.docker.internal:30083` which works from Docker containers. The NodePort works container-to-K8s, just not from Windows host directly.

#### **Services Can't Connect to Docker Compose Infrastructure**

**Problem**: K8s pods can't reach PostgreSQL, MongoDB, Redis, RabbitMQ

**Solution**: Ensure host references use `host.docker.internal` in deployments:

```yaml
- name: SPRING_RABBITMQ_HOST
  value: 'host.docker.internal' # NOT 'rabbitmq' or 'localhost'
```

#### **Quick Health Check**

```powershell
# Check all resources
kubectl get all -n trading-system

# Test endpoints (after port-forward)
curl http://localhost:8083/actuator/health
curl http://localhost:8000/health
```

---

### �📊 Deployment Comparison

| Feature          | Docker Compose | Hybrid (Recommended) | K8s Full      |
| ---------------- | -------------- | -------------------- | ------------- |
| Setup Time       | ⭐ 5 min       | ⭐⭐ 15 min          | ⭐⭐⭐ 30 min |
| Autoscaling      | ❌             | ✅ Apps only         | ✅ All        |
| Load Balancing   | ⚠️ Basic       | ✅ Apps              | ✅ All        |
| Database Backups | ✅ Easy        | ✅ Easy              | ⚠️ Complex    |
| Resource Limits  | ⚠️ Manual      | ✅ Apps              | ✅ All        |
| Best For         | Quick dev      | Production-like dev  | Production    |

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
