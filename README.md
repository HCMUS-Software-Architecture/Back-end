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

See [Architecture Overview](./doc/Architecture.md) for detailed design.

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

## Prerequisites - Services Setup

### Remote Services Configuration

This project connects to the following remote services:

| Service    | Provider | Purpose          | Connection Type |
| ---------- | -------- | ---------------- | --------------- |
| PostgreSQL | Railway  | Primary database | JDBC over SSL   |
| MongoDB    | Atlas    | Document storage | MongoDB+SRV     |
| Redis      | Render   | Cache layer      | Redis over SSL  |

**Configured in [application.yml](src/main/resources/application.yml)**

### Redis Cache Verification

Redis is integrated as the caching layer for improved performance. To verify Redis functionality:

#### Using Maven Test

```powershell
# Run Redis integration tests
.\mvnw.cmd test "-Dtest=RedisIntegrationTest" "-Duser.timezone=UTC"
```

**Expected Output:**

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
- ✓ Redis Availability
- ✓ Redis Server Info
- ✓ Cache Operations (SET/GET/DELETE)
- ✓ RedisTemplate Direct Usage
- ✓ Cache Statistics
- ✓ Connection Factory
```

#### Using Application Health Endpoints

Once the application is running, Redis health can be checked via:

- `GET /api/health/redis` - Redis connection status
- `GET /api/health/redis/test` - Test cache operations
- `GET /api/health/redis/stats` - Cache statistics

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

### 3. Build and Run the Application

**Bash (Linux/macOS):**

```bash
# Build the project
./mvnw clean install -DskipTests

# Run the application with UTC timezone
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC"
```

**PowerShell (Windows 10/11):**

```powershell
# Build the project
.\mvnw.cmd clean install -DskipTests

# Run the application with UTC timezone
java "-Duser.timezone=UTC" -jar target\back-end-0.0.1-SNAPSHOT.jar
```

### 4. Verify the Application

**Bash (Linux/macOS):**

```bash
# Health check
curl http://localhost:8081/actuator/health
```

**PowerShell (Windows 10/11):**

```powershell
# Health check (note: authentication required in production)
Invoke-RestMethod -Uri http://localhost:8081/actuator/health
```

> **Expected response:** `{"status":"UP"}`

### 5. Verify Database Connections

**Check Application Logs for:**

- ✅ `HikariPool-1 - Start completed` (PostgreSQL)
- ✅ `Monitor thread successfully connected` (MongoDB)
- ✅ `Configuring RedisTemplate with JSON serialization` (Redis)
- ✅ `Tomcat started on port 8081`

**Run Integration Tests:**

```powershell
# Test Redis functionality
.\mvnw.cmd test "-Dtest=RedisIntegrationTest" "-Duser.timezone=UTC"
```

---

## Redis Integration - Verification Summary

### ✅ Redis Successfully Integrated

**Test Results:**

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0

✓ Redis PING: PONG
✓ Server Version: 7.2.4
✓ Operating System: Linux 6.8.0-1042-aws x86_64
✓ Cache Operations: SET/GET/DELETE all functional
✓ RedisTemplate: Direct operations working
✓ Connection Factory: Active and responding
```

**Cache Configuration:**

- **Default TTL:** 10 minutes (600 seconds)
- **Articles Cache:** 5 minutes
- **Individual Article:** 15 minutes
- **Price Candles:** 1 minute
- **Serialization:** JSON (Jackson)
- **Connection:** SSL/TLS enabled

**Redis Health Endpoints:**

- `GET /api/health/redis` - Connection status and server info
- `GET /api/health/redis/test` - Run cache operation tests
- `GET /api/health/redis/stats` - View cache hit/miss statistics

**Service Class:** `RedisHealthService` provides programmatic access to:

- Connection testing (PING)
- Server information
- Cache operations (SET/GET/DELETE with TTL)
- Cache statistics (hits, misses, hit rate)

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

## Documentation

| Document                                         | Description                                         |
| ------------------------------------------------ | --------------------------------------------------- |
| [Architecture.md](./doc/Architecture.md)         | Evolutionary architecture with phases and rationale |
| [CoreRequirements.md](./doc/CoreRequirements.md) | Business requirements                               |
| [Features.md](./doc/Features.md)                 | Feature specifications and flows                    |
| [ProjectPlan.md](./doc/ProjectPlan.md)           | 9-week implementation plan                          |
| [UseCaseDiagram.md](./doc/UseCaseDiagram.md)     | User interactions and flows                         |
| [Operations.md](./doc/Operations.md)             | Monitoring, CI/CD, Kubernetes                       |

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
