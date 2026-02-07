# Trading Platform — Back-end

A microservices-based cryptocurrency trading platform featuring real-time price charts, automated news crawling, AI-powered sentiment & price prediction, and tiered account management.

## Core Features

| #   | Feature                | Service(s)                         | Description                                                                                                                           |
| --- | ---------------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **News Crawling**      | `crawler-service`, `news-service`  | Multi-source financial news collection with Playwright-based scraping, adaptive HTML structure learning, and RabbitMQ-driven pipeline |
| 2   | **Price Charts**       | `price-service`, `price-collector` | Historical candle data via Binance API, real-time WebSocket streaming (STOMP over RabbitMQ), multi-timeframe & multi-symbol support   |
| 3   | **AI Analysis**        | `analysis-service`                 | Gemini-powered sentiment analysis on news articles, price trend prediction (UP/DOWN/NEUTRAL) combining candlestick + sentiment data   |
| 4   | **Account Management** | `user-service`                     | JWT + Google OAuth authentication, Standard/VIP tiers (VIP unlocks AI analysis), Redis-cached sessions                                |

**Default symbols:** `BTCUSDT`, `ETHUSDT`, `BNBUSDT`

---

## Prerequisites

| Requirement                 | Version |
| --------------------------- | ------- |
| **Docker & Docker Compose** | v2+     |
| **Java JDK**                | 17      |
| **Node.js**                 | 18+     |
| **pnpm**                    | 9+      |
| **Python**                  | 3.11+   |
| **Git**                     | any     |

**API Keys needed:**

- **Google OAuth** — [console.cloud.google.com/apis/credentials](https://console.cloud.google.com/apis/credentials)
- **Gemini API** — [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) (free tier)
- **OpenRouter** — optional, free tier works without key (`sk-or-v1-free`)

---

## Quick Start (Docker Compose)

### 1. Configure environment

```bash
cp .env.example .env
```

Edit `.env` — at minimum set these:

```dotenv
MONGO_INITDB_ROOT_PASSWORD=<strong-password>
REDIS_PASSWORD=<strong-password>
RABBITMQ_DEFAULT_PASS=<strong-password>
JWT_SECRET=<random-64-char-string>
GOOGLE_OAUTH_CLIENT_ID=<your-google-client-id>
GEMINI_API_KEY=<your-gemini-key>
```

Also configure service-specific `.env` files:

```bash
cp crawler-service/.env.example crawler-service/.env
cp analysis-service/.env.example analysis-service/.env
# news-service/.env — same pattern
```

### 2. Start all services

```bash
docker compose up -d --build
```

### 3. Verify

```bash
# Check all containers
docker compose ps

# Eureka dashboard
open http://localhost:8761

# API Gateway health
curl http://localhost:8081/actuator/health

# RabbitMQ management
open http://localhost:15672
```

### 4. Start Frontend (separate repo)

```bash
cd ../Front-end
cp .env.example .env.local
# Set: NEXT_PUBLIC_LOCAL_URL=http://localhost:8081/api
#      NEXT_PUBLIC_WS_URL=ws://localhost:8081/ws
pnpm install
pnpm dev
```

App available at `http://localhost:3000`.

---

## Local Development (without Docker)

Start infrastructure first, then services individually:

```bash
# 1. Infrastructure (MongoDB, Redis, RabbitMQ)
docker compose up -d mongodb redis rabbitmq

# 2. Discovery Server (must start first)
cd discovery-server && ./mvnw spring-boot:run

# 3. Spring Boot services (each in separate terminal)
cd user-service && ./mvnw spring-boot:run
cd price-service && ./mvnw spring-boot:run         # SPRING_PROFILES_ACTIVE=user
cd price-service && SPRING_PROFILES_ACTIVE=collector ./mvnw spring-boot:run  # collector worker

# 4. NestJS services
cd crawler-service && pnpm install && pnpm start:dev
cd news-service && pnpm install && pnpm start:dev

# 5. Python analysis service
cd analysis-service && pip install -r requirements.txt && python main.py

# 6. API Gateway (start last)
cd api-gateway && ./mvnw spring-boot:run
```

---

## Architecture

```
                         ┌─────────────┐
                         │  Front-end  │ :3000
                         │  (Next.js)  │
                         └──────┬──────┘
                                │
                         ┌──────▼──────┐
                    ┌────│ API Gateway │────┐        ┌───────────┐
                    │    │    :8081     │    │   ┌───►│  Eureka   │
                    │    └──────────────┘    │   │    │   :8761   │
                    │           │            │   │    └───────────┘
          ┌─────────▼──┐ ┌─────▼──────┐ ┌──▼───┴──────┐
          │   User     │ │   Price    │ │   Price      │
          │  Service   │ │  Service   │ │  Collector   │
          │   :8082    │ │   :8083    │ │   (worker)   │
          └─────┬──────┘ └─────┬──────┘ └──────┬───────┘
                │              │               │
     ┌──────────┴──────────────┴───────────────┴──────────┐
     │                    MongoDB :27017                   │
     │                    Redis   :6379                    │
     │                    RabbitMQ :5672                    │
     └──────────┬──────────────┬───────────────┬──────────┘
                │              │               │
          ┌─────▼──────┐ ┌────▼───────┐ ┌─────▼──────┐
          │  Crawler   │ │   News     │ │  Analysis  │
          │  Service   │ │  Service   │ │  Service   │
          │ (NestJS)   │ │ (NestJS)   │ │ (FastAPI)  │
          └────────────┘ └────────────┘ └────────────┘
```

### Service Breakdown

| Service              | Tech                               | Port | Role                                                                                       |
| -------------------- | ---------------------------------- | ---- | ------------------------------------------------------------------------------------------ |
| **api-gateway**      | Spring Cloud Gateway (WebFlux)     | 8081 | Routing, CORS, JWT validation, WebSocket proxy, Swagger aggregation                        |
| **discovery-server** | Spring Cloud Netflix Eureka        | 8761 | Service registry for Spring services                                                       |
| **user-service**     | Spring Boot + MongoDB + Redis      | 8082 | Auth (JWT + Google OAuth), user CRUD, subscription tiers                                   |
| **price-service**    | Spring Boot + MongoDB + RabbitMQ   | 8083 | Candle REST API, WebSocket price streaming via STOMP                                       |
| **price-collector**  | Same codebase, `collector` profile | —    | Background worker fetching Binance data, publishes to RabbitMQ                             |
| **crawler-service**  | NestJS + Playwright + BullMQ       | 9090 | Scheduled web scraping, adaptive HTML parsing, publishes articles to RabbitMQ              |
| **news-service**     | NestJS + MongoDB + Redis           | 9091 | News article CRUD, cached queries                                                          |
| **analysis-service** | FastAPI + Gemini + OpenRouter      | 8000 | Sentiment analysis (Gemini), price prediction (LLM), RabbitMQ consumer for auto-processing |

### Infrastructure

| Component    | Port         | Purpose                                                                                  |
| ------------ | ------------ | ---------------------------------------------------------------------------------------- |
| **MongoDB**  | 27017        | Primary data store (users, articles, candles, analysis results)                          |
| **Redis**    | 6379         | Caching (sessions, candle data, articles), BullMQ job queue backend                      |
| **RabbitMQ** | 5672 / 15672 | Message broker (price events, crawled articles → analysis pipeline), STOMP for WebSocket |
| **Nginx**    | 9000         | Optional reverse proxy with WebSocket support                                            |

### Data Flow

1. **Price pipeline:** Binance API → `price-collector` → RabbitMQ → `price-service` → WebSocket (STOMP) → Frontend
2. **News pipeline:** Web sources → `crawler-service` → RabbitMQ → `news-service` (store) + `analysis-service` (auto-analyze) → MongoDB → Frontend
3. **AI prediction:** Frontend request → `analysis-service` fetches 100 candles + 24h sentiment → LLM → prediction result (UP/DOWN/NEUTRAL + confidence)

---

## Project Structure

```
Back-end/
├── docker-compose.yml          # Full stack orchestration
├── .env.example                # Environment template
├── api-gateway/                # Spring Cloud Gateway (WebFlux)
│   └── src/main/
│       ├── java/.../gateway/
│       │   ├── config/         # CORS, Security, Swagger, WebSocket
│       │   ├── filter/         # JWT auth filter
│       │   └── util/           # JWT utility
│       └── resources/
│           └── application.yml # Route definitions
├── discovery-server/           # Eureka Server
├── user-service/               # Auth & user management
│   └── src/main/java/.../user/
│       ├── controller/         # Auth, User, Subscription endpoints
│       ├── model/              # User, Role, Subscription entities
│       ├── repository/         # MongoDB repositories
│       ├── service/            # Business logic
│       ├── security/           # JWT provider, filters
│       └── config/             # Redis, Security config
├── price-service/              # Price data & WebSocket
│   └── src/main/java/.../price/
│       ├── controller/         # REST + WebSocket endpoints
│       ├── model/              # Candle, Symbol entities
│       ├── collector/          # Binance data fetcher (collector profile)
│       ├── messaging/          # RabbitMQ producer/consumer
│       └── websocket/          # STOMP config & handlers
├── crawler-service/            # NestJS web scraper
│   └── src/
│       ├── crawler/            # Playwright scraping logic
│       ├── scheduler/          # Cron-based scheduling
│       ├── queue/              # BullMQ job processing
│       └── rabbitmq/           # Article publishing
├── news-service/               # NestJS news API
│   └── src/
│       ├── articles/           # Article CRUD module
│       └── cache/              # Redis caching
├── analysis-service/           # Python FastAPI
│   ├── main.py                 # App entry, endpoints
│   ├── services/               # Sentiment & prediction logic
│   ├── models/                 # Pydantic schemas
│   ├── repositories/           # MongoDB data access
│   ├── messaging/              # RabbitMQ consumer
│   ├── config/                 # Settings, prompts
│   └── database/               # MongoDB connection
└── nginx/
```

---

## API Routes (via Gateway :8081)

| Method   | Path                                    | Service          | Auth      |
| -------- | --------------------------------------- | ---------------- | --------- |
| POST     | `/api/auth/register`, `/api/auth/login` | user-service     | No        |
| POST     | `/api/auth/google`                      | user-service     | No        |
| GET      | `/api/user/profile`                     | user-service     | JWT       |
| GET/POST | `/api/subscription/**`                  | user-service     | JWT       |
| GET      | `/api/prices/candles/{symbol}`          | price-service    | JWT       |
| WS       | `/ws/prices`                            | price-service    | —         |
| GET      | `/api/news/**`                          | news-service     | JWT       |
| GET      | `/api/crawler/**`                       | crawler-service  | JWT       |
| POST     | `/api/analysis/analyze`                 | analysis-service | JWT (VIP) |
| POST     | `/api/predict/price`                    | analysis-service | JWT (VIP) |
| GET      | `/api/sentiment/**`                     | analysis-service | JWT       |

---

## Environment Variables Reference

| Variable                     | Required | Default                   | Description                    |
| ---------------------------- | -------- | ------------------------- | ------------------------------ |
| `MONGO_INITDB_ROOT_PASSWORD` | ✅       | —                         | MongoDB root password          |
| `MONGODB_DATABASE`           | —        | `trading`                 | Database name                  |
| `REDIS_PASSWORD`             | ✅       | —                         | Redis auth password            |
| `RABBITMQ_DEFAULT_PASS`      | ✅       | `guest`                   | RabbitMQ password              |
| `JWT_SECRET`                 | ✅       | —                         | JWT signing key (min 32 chars) |
| `JWT_ACCESS_EXPIRATION`      | —        | `900000`                  | Access token TTL (ms)          |
| `JWT_REFRESH_EXPIRATION`     | —        | `604800000`               | Refresh token TTL (ms)         |
| `GOOGLE_OAUTH_CLIENT_ID`     | ✅       | —                         | Google OAuth client ID         |
| `GEMINI_API_KEY`             | ✅       | —                         | Google Gemini API key          |
| `GEMINI_MODEL`               | —        | `gemini-2.0-flash`        | Gemini model name              |
| `OPENROUTER_API_KEY`         | —        | `sk-or-v1-free`           | OpenRouter key (free tier)     |
| `PRICE_SYMBOLS`              | —        | `btcusdt,ethusdt,bnbusdt` | Symbols to track               |
| `CORS_ALLOWED_ORIGINS`       | —        | `http://localhost:3000`   | Allowed CORS origins           |

---

## Useful Commands

```bash
# Logs
docker compose logs -f <service-name>

# Restart single service
docker compose restart price-service

# Scale price-service replicas
docker compose up -d --scale price-service=3

# Rebuild single service
docker compose up -d --build crawler-service

# Stop everything
docker compose down

# Stop & remove volumes (full reset)
docker compose down -v
```
