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

## Quick Start

### 1. Clone the Repository

**Bash (Linux/macOS):**

```bash
git clone https://github.com/HCMUS-Software-Architecture/Back-end.git
cd Back-end
```

**PowerShell (Windows 10/11):**

```powershell
git clone https://github.com/HCMUS-Software-Architecture/Back-end.git
Set-Location Back-end
```

### 2. Configure Application Settings

**Copy the example configuration file:**

```powershell
# PowerShell (Windows)
Copy-Item src\main\resources\application.yml.example src\main\resources\application.yml

# Bash (Linux/macOS)
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

**Set environment variables with your remote PostgreSQL credentials:**

```powershell
# PowerShell (Windows) - Set for current session
$env:PG_URI="jdbc:postgresql://your-host:port/database-name?serverTimezone=UTC"
$env:PG_USERNAME="your_username"
$env:PG_PASSWORD="your_password"
$env:SERVER_PORT="8081"
$env:SHOW_SQL="true"
$env:DDL_AUTO="update"

# Bash (Linux/macOS) - Set for current session
export PG_URI="jdbc:postgresql://your-host:port/database-name?serverTimezone=UTC"
export PG_USERNAME="your_username"
export PG_PASSWORD="your_password"
export SERVER_PORT="8081"
export SHOW_SQL="true"
export DDL_AUTO="update"
```

### 3. Build and Run the Application

**Important:** Always include the `-Duser.timezone=UTC` JVM argument to avoid timezone issues with PostgreSQL.

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
