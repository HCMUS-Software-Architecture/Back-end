# Phase 5: Microservices Preparation - Implementation Guide

**Target Scale**: 10,000+ users  
**Architecture**: Independent microservices with container orchestration  
**Duration**: Week 9 (January 25 - January 31, 2026)

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Environment Setup](#environment-setup)
4. [Service Containerization](#service-containerization)
5. [TimescaleDB Migration](#timescaledb-migration)
6. [Service Discovery](#service-discovery)
7. [API Documentation](#api-documentation)
8. [Performance Optimization](#performance-optimization)
9. [UI/UX Polish & Optimization](#uiux-polish--optimization)
10. [Deployment Preparation](#deployment-preparation)
11. [Final Integration Testing](#final-integration-testing)
12. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
13. [References](#references)

---

## Overview

### Goals

- Containerize all services with Docker
- Create comprehensive API documentation (OpenAPI specs)
- Prepare for Kubernetes deployment
- Conduct final integration testing
- Performance tuning and optimization
- Create deployment documentation
- Migrate to TimescaleDB for optimized time-series queries
- Finalize UI/UX polish and accessibility compliance

### Core Requirements Reference

This phase implements requirements from [CoreRequirements.md](../core/CoreRequirements.md):

1. **Scalable Architecture** - Multi-user requirements with container orchestration
2. **Multiple Timeframes** - Optimized time-series queries with TimescaleDB
3. **Professional Interface** - Final UI/UX polish for production

### Database Strategy Reference

From [DatabaseDesign.md](../core/DatabaseDesign.md) - Phase 5 additions:

| Technology | Purpose | Phase 5 Migration |
|------------|---------|-------------------|
| **TimescaleDB** | Optimized time-series | Hypertables for price_candles |
| **Continuous Aggregates** | Pre-computed rollups | 1h, 4h, 1d candles |

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Load Balancer                                  │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
┌────────────────────────────────┴────────────────────────────────────────┐
│                           API Gateway                                    │
│                      (Rate limiting, Routing)                            │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
     ┌──────────┬────────────────┼────────────────┬──────────┐
     │          │                │                │          │
┌────┴────┐ ┌───┴───┐ ┌──────────┴────────┐ ┌─────┴────┐ ┌───┴────┐
│   API   │ │Crawler│ │  Price Service    │ │   NLP    │ │ Auth   │
│ Service │ │Service│ │ (Collector+Agg)   │ │ Service  │ │Service │
└────┬────┘ └───┬───┘ └────────┬──────────┘ └────┬─────┘ └───┬────┘
     │          │              │                 │           │
     └──────────┴──────────────┼─────────────────┴───────────┘
                               │
                        ┌──────┴──────┐
                        │    Kafka    │
                        └──────┬──────┘
                               │
┌──────────────────────────────┴──────────────────────────────┐
│                         Data Layer                           │
│   PostgreSQL   │   MongoDB   │   Redis   │   TimescaleDB     │
└──────────────────────────────────────────────────────────────┘
```

### Epics for Phase 5

| Epic | Description | Priority |
|------|-------------|----------|
| E5.1 | Docker Image Creation | High |
| E5.2 | OpenAPI Documentation | High |
| E5.3 | Performance Optimization | High |
| E5.4 | Kubernetes Manifests | Medium |
| E5.5 | Final Integration Testing | High |
| E5.6 | Deployment Documentation | High |
| E5.7 | TimescaleDB Migration | High |
| E5.8 | UI/UX Final Polish | Medium |

---

## Prerequisites

### Software Requirements

Ensure all Phase 1-4 prerequisites are installed, plus:

| Software | Version | Purpose |
|----------|---------|---------|
| Kubernetes/kubectl | 1.28+ | Container orchestration |
| Docker Buildx | Latest | Multi-platform builds |
| Helm | 3.x | Kubernetes package manager |

---

## Environment Setup

### Step 1: Update Environment Variables

Add to `.env` file:

```bash
# TimescaleDB Configuration
TIMESCALE_ENABLED=true
TIMESCALE_CHUNK_INTERVAL=1 week

# Production Database URLs
PROD_POSTGRES_URL=postgresql://user:pass@host:5432/trading
PROD_MONGODB_URI=mongodb+srv://user:pass@cluster/trading
PROD_REDIS_URL=redis://user:pass@host:6379

# Docker Registry
DOCKER_REGISTRY=your-registry.io
IMAGE_TAG=v1.0.0

# Kubernetes
K8S_NAMESPACE=trading-platform
K8S_REPLICA_COUNT=3
```

### Step 2: Verify Phase 4 Services

**Bash (Linux/macOS):**
```bash
cd docker
docker compose up -d

# Verify all services are healthy
docker compose ps --format "table {{.Name}}\t{{.Status}}"

# Check Kafka topics
docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location docker
docker compose up -d

# Verify all services are healthy
docker compose ps

# Check Kafka topics
docker exec trading-kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

## Service Containerization

### Step 1: Create Dockerfile

Create `Dockerfile` in project root:

```dockerfile
# Multi-stage build for smaller image
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Step 2: Create Docker Compose for Production

Create `docker/docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  # API Service
  api-service:
    build:
      context: ..
      dockerfile: Dockerfile
    container_name: trading-api
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/trading
      - SPRING_DATA_MONGODB_URI=mongodb://admin:admin@mongodb:27017/trading?authSource=admin
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
    ports:
      - "8081:8080"
    depends_on:
      postgres:
        condition: service_healthy
      mongodb:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M

  # Gateway
  gateway:
    build:
      context: ../gateway
      dockerfile: Dockerfile
    container_name: trading-gateway
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    ports:
      - "8080:8080"
    depends_on:
      - api-service
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Databases
  postgres:
    image: postgres:15
    container_name: trading-postgres
    environment:
      POSTGRES_DB: trading
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  mongodb:
    image: mongo:6.0
    container_name: trading-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-admin}
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: trading-redis
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: trading-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    volumes:
      - zk_data:/var/lib/zookeeper/data

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: trading-kafka
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    volumes:
      - kafka_data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 30s
      timeout: 10s
      retries: 5

volumes:
  postgres_data:
  mongo_data:
  redis_data:
  zk_data:
  kafka_data:
```

### Step 3: Build Docker Images

**Bash (Linux/macOS):**
```bash
# Build main application image
docker build -t trading-platform/api:latest .

# Build gateway image
cd gateway
docker build -t trading-platform/gateway:latest .
cd ..

# List images
docker images | grep trading-platform

# Tag for registry
docker tag trading-platform/api:latest your-registry/trading-platform/api:v1.0.0
docker tag trading-platform/gateway:latest your-registry/trading-platform/gateway:v1.0.0
```

**PowerShell (Windows 10/11):**
```powershell
# Build main application image
docker build -t trading-platform/api:latest .

# Build gateway image
Set-Location gateway
docker build -t trading-platform/gateway:latest .
Set-Location ..

# List images
docker images | Select-String "trading-platform"

# Tag for registry
docker tag trading-platform/api:latest your-registry/trading-platform/api:v1.0.0
docker tag trading-platform/gateway:latest your-registry/trading-platform/gateway:v1.0.0
```

---

## TimescaleDB Migration

This section implements TimescaleDB migration from [DatabaseDesign.md](../core/DatabaseDesign.md).

### Step 1: Update Docker Compose for TimescaleDB

Update `docker/docker-compose.yml` to use TimescaleDB:

```yaml
  postgres:
    image: timescale/timescaledb:latest-pg15
    container_name: trading-postgres
    environment:
      POSTGRES_DB: trading
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-timescale.sql:/docker-entrypoint-initdb.d/02-init-timescale.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Step 2: Create TimescaleDB Migration Script

Create `docker/init-timescale.sql`:

```sql
-- =============================================================================
-- TimescaleDB Migration for Phase 5
-- Reference: DatabaseDesign.md - TimescaleDB Extension (Phase 5+)
-- =============================================================================

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- =============================================================================
-- Convert price_candles to hypertable
-- =============================================================================

-- Create the table if it doesn't exist (migration safe)
CREATE TABLE IF NOT EXISTS trading.price_candles_new (
    id              BIGSERIAL,
    symbol_id       VARCHAR(20) NOT NULL,
    interval        VARCHAR(10) NOT NULL,
    open_time       TIMESTAMPTZ NOT NULL,
    close_time      TIMESTAMPTZ NOT NULL,
    open            DECIMAL(20, 8) NOT NULL,
    high            DECIMAL(20, 8) NOT NULL,
    low             DECIMAL(20, 8) NOT NULL,
    close           DECIMAL(20, 8) NOT NULL,
    volume          DECIMAL(30, 8) NOT NULL,
    quote_volume    DECIMAL(30, 8),
    trade_count     INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_interval CHECK (interval IN ('1m', '5m', '15m', '30m', '1h', '4h', '1d', '1w')),
    PRIMARY KEY (symbol_id, interval, open_time)
);

-- Convert to hypertable
SELECT create_hypertable(
    'trading.price_candles_new', 
    'open_time',
    chunk_time_interval => INTERVAL '1 week',
    if_not_exists => TRUE
);

-- =============================================================================
-- Create Continuous Aggregates for common queries
-- =============================================================================

-- 1-hour continuous aggregate from 1-minute candles
CREATE MATERIALIZED VIEW IF NOT EXISTS trading.candles_1h_agg
WITH (timescaledb.continuous) AS
SELECT 
    symbol_id,
    time_bucket('1 hour', open_time) AS bucket,
    first(open, open_time) AS open,
    max(high) AS high,
    min(low) AS low,
    last(close, open_time) AS close,
    sum(volume) AS volume,
    sum(trade_count) AS trade_count
FROM trading.price_candles_new
WHERE interval = '1m'
GROUP BY symbol_id, bucket
WITH NO DATA;

-- 4-hour continuous aggregate
CREATE MATERIALIZED VIEW IF NOT EXISTS trading.candles_4h_agg
WITH (timescaledb.continuous) AS
SELECT 
    symbol_id,
    time_bucket('4 hours', open_time) AS bucket,
    first(open, open_time) AS open,
    max(high) AS high,
    min(low) AS low,
    last(close, open_time) AS close,
    sum(volume) AS volume,
    sum(trade_count) AS trade_count
FROM trading.price_candles_new
WHERE interval = '1m'
GROUP BY symbol_id, bucket
WITH NO DATA;

-- Daily continuous aggregate
CREATE MATERIALIZED VIEW IF NOT EXISTS trading.candles_1d_agg
WITH (timescaledb.continuous) AS
SELECT 
    symbol_id,
    time_bucket('1 day', open_time) AS bucket,
    first(open, open_time) AS open,
    max(high) AS high,
    min(low) AS low,
    last(close, open_time) AS close,
    sum(volume) AS volume,
    sum(trade_count) AS trade_count
FROM trading.price_candles_new
WHERE interval = '1m'
GROUP BY symbol_id, bucket
WITH NO DATA;

-- =============================================================================
-- Refresh Policies
-- =============================================================================

-- Refresh 1-hour aggregates every 30 minutes
SELECT add_continuous_aggregate_policy('trading.candles_1h_agg',
    start_offset => INTERVAL '1 day',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '30 minutes',
    if_not_exists => TRUE
);

-- Refresh 4-hour aggregates every hour
SELECT add_continuous_aggregate_policy('trading.candles_4h_agg',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '4 hours',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);

-- Refresh daily aggregates every 6 hours
SELECT add_continuous_aggregate_policy('trading.candles_1d_agg',
    start_offset => INTERVAL '7 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '6 hours',
    if_not_exists => TRUE
);

-- =============================================================================
-- Retention Policy
-- =============================================================================

-- Add retention policy: keep 1-minute data for 30 days
SELECT add_retention_policy('trading.price_candles_new', INTERVAL '30 days', if_not_exists => TRUE);

-- =============================================================================
-- Compression Policy (for older chunks)
-- =============================================================================

-- Enable compression on hypertable
ALTER TABLE trading.price_candles_new SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol_id,interval'
);

-- Add compression policy: compress chunks older than 7 days
SELECT add_compression_policy('trading.price_candles_new', INTERVAL '7 days', if_not_exists => TRUE);

-- =============================================================================
-- Verify Setup
-- =============================================================================

-- Check hypertable info
SELECT * FROM timescaledb_information.hypertables;

-- Check continuous aggregates
SELECT * FROM timescaledb_information.continuous_aggregates;

RAISE NOTICE 'TimescaleDB migration complete!';
```

### Step 3: Update Spring Data Configuration

Update `src/main/resources/application-prod.properties`:

```properties
# TimescaleDB Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/trading
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Enable TimescaleDB-optimized queries
app.timescale.enabled=true
app.timescale.chunk-interval=P7D
```

### Step 4: Update Repository for TimescaleDB

Create `src/main/java/com/example/backend/repository/PriceCandleTimescaleRepository.java`:

```java
package com.example.backend.repository;

import com.example.backend.model.PriceCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PriceCandleTimescaleRepository extends JpaRepository<PriceCandle, UUID> {
    
    // Use TimescaleDB time_bucket for efficient aggregation
    @Query(value = """
        SELECT symbol_id, 
               time_bucket(:bucketSize, open_time) AS bucket,
               first(open, open_time) AS open,
               max(high) AS high,
               min(low) AS low,
               last(close, open_time) AS close,
               sum(volume) AS volume
        FROM trading.price_candles_new
        WHERE symbol_id = :symbol
          AND open_time >= :startTime
          AND open_time < :endTime
        GROUP BY symbol_id, bucket
        ORDER BY bucket DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findAggregatedCandles(
        String symbol,
        String bucketSize,
        Instant startTime,
        Instant endTime,
        int limit
    );
    
    // Use continuous aggregate for 1-hour candles
    @Query(value = """
        SELECT symbol_id, bucket, open, high, low, close, volume
        FROM trading.candles_1h_agg
        WHERE symbol_id = :symbol
          AND bucket >= :startTime
        ORDER BY bucket DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> find1HourCandles(String symbol, Instant startTime, int limit);
    
    // Use continuous aggregate for daily candles
    @Query(value = """
        SELECT symbol_id, bucket, open, high, low, close, volume
        FROM trading.candles_1d_agg
        WHERE symbol_id = :symbol
          AND bucket >= :startTime
        ORDER BY bucket DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findDailyCandles(String symbol, Instant startTime, int limit);
}
```

---

## API Documentation

### Step 1: Add OpenAPI Dependencies

Update `pom.xml`:

```xml
<!-- OpenAPI/Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### Step 2: Configure OpenAPI

Create `src/main/java/com/example/backend/config/OpenApiConfig.java`:

```java
package com.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI tradingPlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                    .title("Trading Platform API")
                    .description("Financial news aggregation, real-time price charts, and AI-powered analysis")
                    .version("1.0.0")
                    .contact(new Contact()
                        .name("HCMUS Software Architecture Team")
                        .email("team@example.com"))
                    .license(new License()
                        .name("Educational Use")
                        .url("https://github.com/HCMUS-Software-Architecture/Back-end")))
                .servers(List.of(
                    new Server().url("http://localhost:8080").description("Development"),
                    new Server().url("https://api.trading.example.com").description("Production")
                ));
    }
}
```

### Step 3: Annotate Controllers

Update controllers with OpenAPI annotations:

```java
package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
// ... other imports

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Articles", description = "News article management endpoints")
@RequiredArgsConstructor
public class ArticleController {
    
    @Operation(summary = "Get all articles", description = "Returns paginated list of articles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved articles"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    @GetMapping
    public ResponseEntity<Page<ArticleDocument>> getAllArticles(
            @Parameter(description = "Page number (0-indexed)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "20") int size
    ) {
        // ... implementation
    }
}
```

### Step 4: Access API Documentation

After starting the application:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

---

## Performance Optimization

### Step 1: Connection Pooling

Update `src/main/resources/application-prod.properties`:

```properties
# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=30000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1800000

# MongoDB Connection Pool
spring.data.mongodb.option.max-connection-pool-size=50
spring.data.mongodb.option.min-connection-pool-size=10

# Redis Connection Pool
spring.data.redis.lettuce.pool.max-active=50
spring.data.redis.lettuce.pool.max-idle=10
spring.data.redis.lettuce.pool.min-idle=5
```

### Step 2: JVM Tuning

Create `docker/jvm.options`:

```
# Memory settings
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:InitialRAMPercentage=50.0

# GC settings
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled

# Monitoring
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof

# JIT compilation
-XX:+TieredCompilation
-XX:TieredStopAtLevel=1
```

### Step 3: Database Query Optimization

Add indexes to frequently queried fields:

```sql
-- PostgreSQL indexes for price data
CREATE INDEX CONCURRENTLY idx_price_ticks_symbol_time 
ON price_ticks (symbol, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_price_candles_symbol_interval_time 
ON price_candles (symbol, interval, open_time DESC);

-- Partial index for recent data
CREATE INDEX CONCURRENTLY idx_price_ticks_recent 
ON price_ticks (symbol, timestamp) 
WHERE timestamp > NOW() - INTERVAL '7 days';
```

---

## UI/UX Polish & Optimization

This section implements final UI/UX improvements following [UIUXGuidelines.md](../core/UIUXGuidelines.md).

### Step 1: Performance Optimization

Create `frontend/src/lib/performance.ts`:

```typescript
// Lazy loading for chart components
import dynamic from 'next/dynamic';

export const LazyPriceChart = dynamic(
  () => import('@/components/chart/StreamingPriceChart').then(mod => mod.StreamingPriceChart),
  {
    loading: () => <div className="h-[400px] animate-pulse bg-muted rounded-lg" />,
    ssr: false, // Charts need client-side rendering
  }
);

export const LazyNewsFeed = dynamic(
  () => import('@/components/news/LiveNewsFeed').then(mod => mod.LiveNewsFeed),
  {
    loading: () => <div className="h-[300px] animate-pulse bg-muted rounded-lg" />,
  }
);
```

### Step 2: Accessibility Audit Checklist

Ensure WCAG 2.1 AA compliance:

| Requirement | Implementation | Status |
|-------------|----------------|--------|
| **Color Contrast** | 4.5:1 minimum for text | ✅ |
| **Focus Indicators** | Visible focus rings | ✅ |
| **Keyboard Navigation** | Full Tab support | ✅ |
| **Screen Reader** | ARIA labels | ✅ |
| **Motion Preference** | Reduced motion support | ⬜ |

Add reduced motion support:

```css
/* In globals.css */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
  
  /* Keep essential price updates visible */
  .price-flash {
    animation: none !important;
    transition: background-color 0.5s ease-out !important;
  }
}
```

### Step 3: Error States & Empty States

Create `frontend/src/components/states/ErrorBoundary.tsx`:

```tsx
'use client';

import React, { Component, ReactNode } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <Card className="border-destructive">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <AlertTriangle className="h-12 w-12 text-destructive mb-4" />
            <h3 className="text-lg font-semibold mb-2">Something went wrong</h3>
            <p className="text-muted-foreground text-center mb-4 max-w-md">
              {this.state.error?.message || 'An unexpected error occurred'}
            </p>
            <Button
              variant="outline"
              onClick={() => this.setState({ hasError: false })}
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              Try again
            </Button>
          </CardContent>
        </Card>
      );
    }

    return this.props.children;
  }
}
```

Create `frontend/src/components/states/EmptyState.tsx`:

```tsx
import React from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center justify-center py-12">
        <Icon className="h-12 w-12 text-muted-foreground/50 mb-4" />
        <h3 className="text-lg font-semibold mb-2">{title}</h3>
        <p className="text-muted-foreground text-center mb-4 max-w-md">
          {description}
        </p>
        {action && (
          <Button onClick={action.onClick}>
            {action.label}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
```

### Step 4: Mobile Responsiveness

Update `frontend/tailwind.config.ts` for mobile breakpoints:

```typescript
import type { Config } from 'tailwindcss';

const config: Config = {
  // ... existing config
  theme: {
    extend: {
      screens: {
        'xs': '475px',
        // Default breakpoints: sm: 640px, md: 768px, lg: 1024px, xl: 1280px
      },
      spacing: {
        'safe-top': 'env(safe-area-inset-top)',
        'safe-bottom': 'env(safe-area-inset-bottom)',
      },
    },
  },
};

export default config;
```

### Step 5: Final Production Checklist

| Category | Item | Status |
|----------|------|--------|
| **Performance** | Lighthouse score > 90 | ⬜ |
| **SEO** | Meta tags configured | ⬜ |
| **Accessibility** | WCAG 2.1 AA compliant | ⬜ |
| **Security** | CSP headers configured | ⬜ |
| **PWA** | Service worker (optional) | ⬜ |
| **Analytics** | Error tracking enabled | ⬜ |

---

## Deployment Preparation

### Kubernetes Manifests

Create `k8s/namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: trading-platform
```

Create `k8s/api-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trading-api
  namespace: trading-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: trading-api
  template:
    metadata:
      labels:
        app: trading-api
    spec:
      containers:
      - name: api
        image: trading-platform/api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: trading-secrets
              key: database-url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: trading-api
  namespace: trading-platform
spec:
  selector:
    app: trading-api
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

Create `k8s/hpa.yaml`:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: trading-api-hpa
  namespace: trading-platform
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: trading-api
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## Final Integration Testing

### Performance Benchmark Script

**Bash (Linux/macOS):**
```bash
#!/bin/bash
echo "=== Phase 5 Performance Benchmark ==="

# Start services
cd docker
docker compose -f docker-compose.prod.yml up -d
cd ..

# Wait for services
echo "Waiting for services to start..."
sleep 60

# Run benchmarks
echo "Running benchmarks..."

# API Latency Test
echo "1. API Latency Test (100 requests)"
for i in {1..100}; do
    curl -s -o /dev/null -w "%{time_total}\n" http://localhost:8080/api/health
done | awk '{sum+=$1; count++} END {print "   Average: " sum/count*1000 " ms"}'

# Throughput Test
echo "2. Throughput Test (10 seconds)"
ab -n 1000 -c 50 -t 10 http://localhost:8080/api/articles?page=0\&size=10 2>/dev/null | grep "Requests per second"

# WebSocket Connection Test
echo "3. WebSocket Connection Test"
echo "   Testing 100 concurrent connections..."
# (Use k6 or similar tool)

echo "=== Benchmark Complete ==="
```

**PowerShell (Windows 10/11):**
```powershell
Write-Host "=== Phase 5 Performance Benchmark ===" -ForegroundColor Cyan

# Start services
Set-Location docker
docker compose -f docker-compose.prod.yml up -d
Set-Location ..

# Wait for services
Write-Host "Waiting for services to start..."
Start-Sleep -Seconds 60

# Run benchmarks
Write-Host "Running benchmarks..."

# API Latency Test
Write-Host "1. API Latency Test (100 requests)"
$times = @()
for ($i = 0; $i -lt 100; $i++) {
    $start = Get-Date
    Invoke-RestMethod -Uri http://localhost:8080/api/health -ErrorAction SilentlyContinue
    $end = Get-Date
    $times += ($end - $start).TotalMilliseconds
}
$avg = ($times | Measure-Object -Average).Average
Write-Host "   Average: $($avg.ToString('F2')) ms"

# Note: For throughput testing, use k6 or Apache Benchmark on Windows
Write-Host "2. For throughput testing, run: k6 run tests/performance/load-test-phase4-full.js"

Write-Host "=== Benchmark Complete ===" -ForegroundColor Cyan
```

---

## Common Pitfalls & Troubleshooting

### Issue 1: Container OOM Killed

**Symptoms**: Container restarts with exit code 137

**Solutions**:
1. Increase memory limits in docker-compose
2. Configure JVM memory properly:
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

### Issue 2: Slow Container Startup

**Symptoms**: Health checks fail, container marked unhealthy

**Solutions**:
1. Increase `initialDelaySeconds` in health checks
2. Use multi-stage builds to reduce image size
3. Pre-warm the JVM with `-XX:TieredStopAtLevel=1`

### Issue 3: Database Connection Pool Exhaustion

**Symptoms**: `HikariPool-1 - Connection is not available`

**Solutions**:
1. Increase pool size
2. Reduce connection timeout
3. Add connection leak detection:
```properties
spring.datasource.hikari.leak-detection-threshold=60000
```

---

## References

### Official Documentation

- [Docker Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot Docker](https://spring.io/guides/topicals/spring-boot-docker/)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [TimescaleDB Documentation](https://docs.timescale.com/)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

### Related Project Documents

- [CoreRequirements.md](../core/CoreRequirements.md) - Business requirements
- [DatabaseDesign.md](../core/DatabaseDesign.md) - Database architecture (TimescaleDB)
- [UIUXGuidelines.md](../core/UIUXGuidelines.md) - UI/UX design guidelines
- [Phase4-ImplementationGuide.md](./Phase4-ImplementationGuide.md) - Previous phase

### GitHub Resources

- [TradingView Charting Library](https://www.tradingview.com/charting-library-docs/)
- [shadcn/ui Components](https://ui.shadcn.com/)
