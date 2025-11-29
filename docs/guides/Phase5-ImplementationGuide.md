# Phase 5: Microservices Preparation - Implementation Guide

**Target Scale**: 10,000+ users  
**Architecture**: Independent microservices with container orchestration  
**Duration**: Week 9 (January 25 - January 31, 2026)

---

## Table of Contents

1. [Overview](#overview)
2. [Service Containerization](#service-containerization)
3. [Service Discovery](#service-discovery)
4. [API Documentation](#api-documentation)
5. [Performance Optimization](#performance-optimization)
6. [Deployment Preparation](#deployment-preparation)
7. [Final Integration Testing](#final-integration-testing)
8. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
9. [References](#references)

---

## Overview

### Goals

- Containerize all services with Docker
- Create comprehensive API documentation (OpenAPI specs)
- Prepare for Kubernetes deployment
- Conduct final integration testing
- Performance tuning and optimization
- Create deployment documentation

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

### Related Project Documents

- [Architecture.md](../Architecture.md) - System architecture
- [Operations.md](../Operations.md) - Monitoring and CI/CD
- [Phase5-TestingGuide.md](./Phase5-TestingGuide.md) - Testing strategies
