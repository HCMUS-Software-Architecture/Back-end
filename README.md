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

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 17+ | Backend runtime |
| Maven | 3.8+ | Build tool |
| Docker | 20.10+ | Container runtime |
| Docker Compose | 2.0+ | Multi-container orchestration |
| Node.js | 18+ | Frontend runtime (for Next.js) |
| Git | 2.30+ | Version control |

### Recommended Tools

| Tool | Purpose |
|------|---------|
| IntelliJ IDEA / VS Code | IDE |
| Docker Desktop | Container management UI |
| Postman / Insomnia | API testing |
| DBeaver | Database management |

### Verify Installation

**Bash (Linux/macOS):**
```bash
# Check Java
java -version
# Expected: openjdk version "17.x.x" or higher

# Check Maven
mvn -version
# Expected: Apache Maven 3.8.x or higher

# Check Docker
docker --version
# Expected: Docker version 20.10.x or higher

# Check Docker Compose
docker compose version
# Expected: Docker Compose version v2.x.x

# Check Node.js
node --version
# Expected: v18.x.x or higher
```

**PowerShell (Windows 10/11):**
```powershell
# Check Java
java -version
# Expected: openjdk version "17.x.x" or higher

# Check Maven
mvn -version
# Expected: Apache Maven 3.8.x or higher

# Check Docker
docker --version
# Expected: Docker version 20.10.x or higher

# Check Docker Compose
docker compose version
# Expected: Docker Compose version v2.x.x

# Check Node.js
node --version
# Expected: v18.x.x or higher
```

---

## Quick Start

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
# Start infrastructure services (PostgreSQL, etc.)
docker compose up -d

# Build the application
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

**PowerShell (Windows 10/11):**
```powershell
# Start infrastructure services (PostgreSQL, etc.)
docker compose up -d

# Build the application
.\mvnw.cmd clean install

# Run the application
.\mvnw.cmd spring-boot:run
```

### 4. Verify the Application

**Bash (Linux/macOS):**
```bash
# Health check
curl http://localhost:8080/actuator/health
```

**PowerShell (Windows 10/11):**
```powershell
# Health check
Invoke-RestMethod -Uri http://localhost:8080/actuator/health
```

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
│   │       ├── application.properties     # Application configuration
│   │       ├── application-dev.properties # Development profile
│   │       └── application-prod.properties# Production profile
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

## Development Setup

### IDE Configuration

#### IntelliJ IDEA

1. Open the project: `File > Open > Select project root`
2. Import as Maven project when prompted
3. Enable annotation processing: `Settings > Build > Compiler > Annotation Processors > Enable`
4. Set JDK: `Project Structure > Project > SDK > Java 17+`

#### Visual Studio Code

1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Docker
2. Open the project folder
3. VS Code will automatically detect Maven project

### Environment Variables

Create a `.env` file in the project root (not committed to Git):

**Bash (Linux/macOS) - Create .env file:**
```bash
cat > .env << 'EOF'
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=trading
DB_USER=postgres
DB_PASSWORD=postgres

# Exchange API (Phase 3+)
BINANCE_API_KEY=your_api_key
BINANCE_API_SECRET=your_api_secret

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
EOF
```

**PowerShell (Windows 10/11) - Create .env file:**
```powershell
@"
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=trading
DB_USER=postgres
DB_PASSWORD=postgres

# Exchange API (Phase 3+)
BINANCE_API_KEY=your_api_key
BINANCE_API_SECRET=your_api_secret

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
"@ | Out-File -FilePath .env -Encoding utf8
```

### Database Setup

**Bash (Linux/macOS):**
```bash
# Start PostgreSQL with Docker
docker run -d \
  --name trading-db \
  -e POSTGRES_DB=trading \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

# Verify connection
docker exec -it trading-db psql -U postgres -d trading
```

**PowerShell (Windows 10/11):**
```powershell
# Start PostgreSQL with Docker
docker run -d `
  --name trading-db `
  -e POSTGRES_DB=trading `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  postgres:15

# Verify connection
docker exec -it trading-db psql -U postgres -d trading
```

---

## Configuration

### Application Profiles

| Profile | Purpose | Activation |
|---------|---------|------------|
| `default` | Local development with H2 | Default |
| `dev` | Development with PostgreSQL | `-Dspring.profiles.active=dev` |
| `prod` | Production settings | `-Dspring.profiles.active=prod` |

### Configuration Files

| File | Purpose |
|------|---------|
| `application.properties` | Base configuration |
| `application-dev.properties` | Development overrides |
| `application-prod.properties` | Production overrides |

---

## Running the Application

### Development Mode

**Bash (Linux/macOS):**
```bash
# Using Maven wrapper
./mvnw spring-boot:run

# With specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# With debug enabled
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

**PowerShell (Windows 10/11):**
```powershell
# Using Maven wrapper
.\mvnw.cmd spring-boot:run

# With specific profile
.\mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=dev"

# With debug enabled
.\mvnw.cmd spring-boot:run -D"spring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Building for Production

**Bash (Linux/macOS):**
```bash
# Build JAR
./mvnw clean package -DskipTests

# Run JAR
java -jar target/back-end-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

**PowerShell (Windows 10/11):**
```powershell
# Build JAR
.\mvnw.cmd clean package -DskipTests

# Run JAR
java -jar target\back-end-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker (Phase 2+)

**Bash (Linux/macOS):**
```bash
# Build Docker image
docker build -t trading-platform/api:latest .

# Run with Docker Compose
docker compose up -d
```

**PowerShell (Windows 10/11):**
```powershell
# Build Docker image
docker build -t trading-platform/api:latest .

# Run with Docker Compose
docker compose up -d
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture.md](./doc/Architecture.md) | Evolutionary architecture with phases and rationale |
| [CoreRequirements.md](./doc/CoreRequirements.md) | Business requirements |
| [Features.md](./doc/Features.md) | Feature specifications and flows |
| [ProjectPlan.md](./doc/ProjectPlan.md) | 9-week implementation plan |
| [UseCaseDiagram.md](./doc/UseCaseDiagram.md) | User interactions and flows |
| [Operations.md](./doc/Operations.md) | Monitoring, CI/CD, Kubernetes |

---

## Technology Stack

### Backend

| Technology | Purpose |
|------------|---------|
| Java 17+ | Runtime |
| Spring Boot 3.x | Application framework |
| Spring Data JPA | Database access |
| Spring WebSocket | Real-time communication |
| Lombok | Boilerplate reduction |

### Databases (per phase)

| Phase | Databases |
|-------|-----------|
| 1 | PostgreSQL (JSONB) |
| 2+ | PostgreSQL + MongoDB + Redis |
| 4+ | + Apache Kafka |
| 5+ | + TimescaleDB |

### Frontend

| Technology | Purpose |
|------------|---------|
| Next.js | React framework |
| TradingView Charts | Price visualization |
| WebSocket | Real-time updates |

---

## API Reference

### Health Check

```http
GET /actuator/health
```

### Articles (Phase 2+)

```http
GET /api/articles
GET /api/articles/{id}
```

### Prices (Phase 3+)

```http
GET /api/prices/historical?symbol=BTCUSDT&timeframe=1h
WS  /ws/prices
```

---

## Contributing

1. Create a feature branch from `develop`
2. Make changes following code style guidelines
3. Write tests for new functionality
4. Submit a pull request

---

## License

This project is for educational purposes at HCMUS.

---

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [TradingView Charting Library](https://www.tradingview.com/chart/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

