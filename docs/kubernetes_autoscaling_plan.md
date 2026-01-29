# Kubernetes Autoscaling Plan for Trading Platform

**Document Version**: 1.0  
**Date**: January 26, 2026  
**Prerequisites**:

- [answer.md](./answer.md) - Architecture Q&A (Section 3: K8s Scaling)
- [todo_list_part_5.md](./todo_list_part_5.md) - Part 5 TODO List

**Focus**: Kubernetes deployment, HPA configuration for WebSocket-based price-service, complete architecture diagram

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current Docker Compose Architecture](#2-current-docker-compose-architecture)
3. [Target Kubernetes Architecture](#3-target-kubernetes-architecture)
4. [Kubernetes Manifests](#4-kubernetes-manifests)
5. [Autoscaling Configuration](#5-autoscaling-configuration)
6. [Custom Metrics for WebSocket](#6-custom-metrics-for-websocket)
7. [Ingress & Load Balancing](#7-ingress--load-balancing)
8. [RabbitMQ Integration](#8-rabbitmq-integration)
9. [Complete Architecture Diagram](#9-complete-architecture-diagram)
10. [Implementation Steps](#10-implementation-steps)
11. [Monitoring & Observability](#11-monitoring--observability)
12. [Disaster Recovery](#12-disaster-recovery)

---

## 1. Executive Summary

### Why Kubernetes for Price Service?

The price-service is the most critical component for scalability because:

- **WebSocket Connections**: Each user maintains a persistent connection
- **Real-time Requirements**: Price updates must be delivered with minimal latency (<100ms)
- **Traffic Patterns**: Highly variable (market hours vs. off-hours)
- **State Management**: WebSocket sessions are stateful

### Scaling Goals

| Metric                        | Target      | Current Capacity  |
| ----------------------------- | ----------- | ----------------- |
| Max Concurrent Users          | 10,000      | ~500 (2 replicas) |
| WebSocket Connections per Pod | 500         | Not measured      |
| P99 Latency                   | <100ms      | Unknown           |
| Scale-up Time                 | <60 seconds | N/A               |
| Scale-down Time               | <5 minutes  | N/A               |

### Key Decisions

1. **Use KEDA** for custom WebSocket metrics-based autoscaling
2. **NGINX Ingress** with sticky sessions for WebSocket affinity
3. **Redis** for WebSocket session state (enables graceful scaling)
4. **Prometheus** for metrics collection and alerting

---

## 2. Current Docker Compose Architecture

### Current State (from docker-compose.yml)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CURRENT DOCKER COMPOSE SETUP                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌─────────────┐         ┌─────────────┐         ┌─────────────┐       │
│   │  PostgreSQL │         │   MongoDB   │         │    Redis    │       │
│   │    :5432    │         │   :27017    │         │    :6379    │       │
│   └─────────────┘         └─────────────┘         └─────────────┘       │
│                                                                          │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                        RabbitMQ :15672, :3001                    │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐    │
│   │  Discovery  │    │ API Gateway │    │     Price Service       │    │
│   │   Server    │    │   :8081     │    │  :8083 (replicas: 2)    │    │
│   │   :8761     │    │             │    │                         │    │
│   └─────────────┘    └─────────────┘    └─────────────────────────┘    │
│                                                                          │
│   ┌─────────────┐    ┌─────────────┐                                   │
│   │    User     │    │   Price     │                                   │
│   │   Service   │    │  Collector  │                                   │
│   │   :8082     │    │   :8083     │                                   │
│   └─────────────┘    └─────────────┘                                   │
│                                                                          │
│   Limitations:                                                           │
│   - Fixed replicas (manual scaling)                                     │
│   - No auto-healing                                                     │
│   - No metrics-based scaling                                            │
│   - Single host deployment                                              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Target Kubernetes Architecture

### High-Level Target State

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          TARGET KUBERNETES ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │                              KUBERNETES CLUSTER                                │  │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │                           INGRESS LAYER                                  │  │  │
│  │  │   ┌─────────────────────────────────────────────────────────────────┐   │  │  │
│  │  │   │            NGINX Ingress Controller (LoadBalancer)              │   │  │  │
│  │  │   │   - TLS Termination      - WebSocket Upgrade                    │   │  │  │
│  │  │   │   - Sticky Sessions      - Rate Limiting                        │   │  │  │
│  │  │   └─────────────────────────────────────────────────────────────────┘   │  │  │
│  │  └─────────────────────────────────────────────────────────────────────────┘  │  │
│  │                                      │                                         │  │
│  │  ┌───────────────────────────────────┼───────────────────────────────────┐    │  │
│  │  │                     NAMESPACE: trading-system                          │    │  │
│  │  │                                   │                                    │    │  │
│  │  │         ┌─────────────────────────┼─────────────────────────┐         │    │  │
│  │  │         ▼                         ▼                         ▼         │    │  │
│  │  │  ┌─────────────┐          ┌─────────────┐          ┌─────────────┐   │    │  │
│  │  │  │ API Gateway │          │   Price     │          │    User     │   │    │  │
│  │  │  │  Deployment │          │  Service    │          │   Service   │   │    │  │
│  │  │  │  replicas:2 │          │  HPA: 2-20  │          │ replicas:2  │   │    │  │
│  │  │  └─────────────┘          └──────┬──────┘          └─────────────┘   │    │  │
│  │  │                                  │                                    │    │  │
│  │  │                           ┌──────┴──────┐                            │    │  │
│  │  │                           │    KEDA     │                            │    │  │
│  │  │                           │ ScaledObject│                            │    │  │
│  │  │                           │  (Custom    │                            │    │  │
│  │  │                           │  Metrics)   │                            │    │  │
│  │  │                           └─────────────┘                            │    │  │
│  │  │                                                                       │    │  │
│  │  │  ┌─────────────┐          ┌─────────────┐          ┌─────────────┐   │    │  │
│  │  │  │   Price     │          │  Discovery  │          │ Prometheus  │   │    │  │
│  │  │  │  Collector  │          │   Server    │          │  + Grafana  │   │    │  │
│  │  │  │ replicas:1  │          │ replicas:1  │          │             │   │    │  │
│  │  │  └─────────────┘          └─────────────┘          └─────────────┘   │    │  │
│  │  │                                                                       │    │  │
│  │  └───────────────────────────────────────────────────────────────────────┘    │  │
│  │                                                                                │  │
│  │  ┌───────────────────────────────────────────────────────────────────────┐    │  │
│  │  │                     NAMESPACE: trading-data                            │    │  │
│  │  │                                                                        │    │  │
│  │  │   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐              │    │  │
│  │  │   │ PostgreSQL  │    │  RabbitMQ   │    │    Redis    │              │    │  │
│  │  │   │  StatefulSet│    │ StatefulSet │    │ StatefulSet │              │    │  │
│  │  │   │  (HA: 3)    │    │  (HA: 3)    │    │  (HA: 3)    │              │    │  │
│  │  │   └─────────────┘    └─────────────┘    └─────────────┘              │    │  │
│  │  │                                                                        │    │  │
│  │  └───────────────────────────────────────────────────────────────────────┘    │  │
│  │                                                                                │  │
│  └───────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                      │
│  EXTERNAL:                                                                           │
│  ┌─────────────┐    ┌─────────────┐                                                │
│  │ MongoDB     │    │   Binance   │                                                │
│  │ Atlas       │    │     API     │                                                │
│  └─────────────┘    └─────────────┘                                                │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Kubernetes Manifests

### 4.1 Namespace Configuration

**File**: `k8s/namespaces.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: trading-system
  labels:
    name: trading-system
    istio-injection: disabled

---
apiVersion: v1
kind: Namespace
metadata:
  name: trading-data
  labels:
    name: trading-data

---
apiVersion: v1
kind: Namespace
metadata:
  name: trading-monitoring
  labels:
    name: trading-monitoring
```

---

### 4.2 ConfigMaps and Secrets

**File**: `k8s/config/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trading-config
  namespace: trading-system
data:
  EUREKA_SERVER_URL: 'http://discovery-server:8761/eureka'
  RABBITMQ_HOST: 'rabbitmq.trading-data.svc.cluster.local'
  RABBITMQ_PORT: '5672'
  RABBITMQ_STOMP_PORT: '61613'
  REDIS_HOST: 'redis.trading-data.svc.cluster.local'
  REDIS_PORT: '6379'
  POSTGRES_HOST: 'postgresql.trading-data.svc.cluster.local'
  POSTGRES_PORT: '5432'
  LOG_LEVEL: 'INFO'
```

**File**: `k8s/config/secrets.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: trading-secrets
  namespace: trading-system
type: Opaque
stringData:
  POSTGRES_PASSWORD: 'postgres123'
  RABBITMQ_PASSWORD: 'guest'
  REDIS_PASSWORD: 'redis123'
  JWT_SECRET: 'your-256-bit-secret-key-here-change-in-production'
  MONGODB_ATLAS_URI: 'mongodb+srv://trading_app:PASSWORD@cluster.mongodb.net/trading_users'
```

---

### 4.3 Price Service Deployment

**File**: `k8s/deployments/price-service.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: price-service
  namespace: trading-system
  labels:
    app: price-service
    tier: backend
spec:
  replicas: 2 # Initial replicas (HPA will manage this)
  selector:
    matchLabels:
      app: price-service
  template:
    metadata:
      labels:
        app: price-service
        tier: backend
      annotations:
        prometheus.io/scrape: 'true'
        prometheus.io/port: '8083'
        prometheus.io/path: '/actuator/prometheus'
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values:
                        - price-service
                topologyKey: kubernetes.io/hostname
      containers:
        - name: price-service
          image: trading-platform/price-service:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8083
              name: http
              protocol: TCP
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes'
            - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: EUREKA_SERVER_URL
            - name: SPRING_RABBITMQ_HOST
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: RABBITMQ_HOST
            - name: SPRING_RABBITMQ_PORT
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: RABBITMQ_STOMP_PORT
            - name: SPRING_RABBITMQ_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: trading-secrets
                  key: RABBITMQ_PASSWORD
            - name: SPRING_REDIS_HOST
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: REDIS_HOST
            - name: SPRING_REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: trading-secrets
                  key: REDIS_PASSWORD
            - name: JAVA_OPTS
              value: '-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xms512m -Xmx1024m'
          resources:
            requests:
              cpu: '250m'
              memory: '512Mi'
            limits:
              cpu: '1000m'
              memory: '1024Mi'
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8083
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8083
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - 'sleep 10' # Allow graceful WebSocket disconnection
      terminationGracePeriodSeconds: 30

---
apiVersion: v1
kind: Service
metadata:
  name: price-service
  namespace: trading-system
  labels:
    app: price-service
spec:
  type: ClusterIP
  ports:
    - port: 8083
      targetPort: 8083
      protocol: TCP
      name: http
  selector:
    app: price-service
```

---

### 4.4 Price Collector Deployment (Singleton)

**File**: `k8s/deployments/price-collector.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: price-collector
  namespace: trading-system
  labels:
    app: price-collector
    tier: backend
spec:
  replicas: 1 # MUST be singleton - connects to external APIs
  strategy:
    type: Recreate # Ensure only one instance at a time
  selector:
    matchLabels:
      app: price-collector
  template:
    metadata:
      labels:
        app: price-collector
        tier: backend
    spec:
      containers:
        - name: price-collector
          image: trading-platform/price-service:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8083
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes,collector'
            - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: EUREKA_SERVER_URL
            - name: SPRING_RABBITMQ_HOST
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: RABBITMQ_HOST
            - name: BINANCE_API_KEY
              valueFrom:
                secretKeyRef:
                  name: trading-secrets
                  key: BINANCE_API_KEY
                  optional: true
            - name: JAVA_OPTS
              value: '-Xms256m -Xmx512m'
          resources:
            requests:
              cpu: '100m'
              memory: '256Mi'
            limits:
              cpu: '500m'
              memory: '512Mi'
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8083
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8083
            initialDelaySeconds: 30
            periodSeconds: 10
```

---

### 4.5 API Gateway Deployment

**File**: `k8s/deployments/api-gateway.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: trading-system
  labels:
    app: api-gateway
    tier: frontend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
        tier: frontend
    spec:
      containers:
        - name: api-gateway
          image: trading-platform/api-gateway:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8081
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes'
            - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: EUREKA_SERVER_URL
            - name: CORS_ALLOWED_ORIGINS
              value: 'https://trading.example.com,http://localhost:3000'
          resources:
            requests:
              cpu: '200m'
              memory: '384Mi'
            limits:
              cpu: '500m'
              memory: '512Mi'
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 45
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 20
            periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: trading-system
spec:
  type: ClusterIP
  ports:
    - port: 8081
      targetPort: 8081
      protocol: TCP
  selector:
    app: api-gateway
```

---

### 4.6 User Service Deployment

**File**: `k8s/deployments/user-service.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: trading-system
  labels:
    app: user-service
    tier: backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
        tier: backend
    spec:
      containers:
        - name: user-service
          image: trading-platform/user-service:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8082
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes,production'
            - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
              valueFrom:
                configMapKeyRef:
                  name: trading-config
                  key: EUREKA_SERVER_URL
            - name: MONGODB_URI
              valueFrom:
                secretKeyRef:
                  name: trading-secrets
                  key: MONGODB_ATLAS_URI
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: trading-secrets
                  key: JWT_SECRET
          resources:
            requests:
              cpu: '200m'
              memory: '384Mi'
            limits:
              cpu: '500m'
              memory: '512Mi'
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8082
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: trading-system
spec:
  type: ClusterIP
  ports:
    - port: 8082
      targetPort: 8082
  selector:
    app: user-service
```

---

### 4.7 Discovery Server Deployment

**File**: `k8s/deployments/discovery-server.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: discovery-server
  namespace: trading-system
  labels:
    app: discovery-server
    tier: infrastructure
spec:
  replicas: 1 # Eureka can be HA, but single is fine for this scale
  selector:
    matchLabels:
      app: discovery-server
  template:
    metadata:
      labels:
        app: discovery-server
        tier: infrastructure
    spec:
      containers:
        - name: discovery-server
          image: trading-platform/discovery-server:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8761
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes'
          resources:
            requests:
              cpu: '100m'
              memory: '256Mi'
            limits:
              cpu: '300m'
              memory: '384Mi'
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8761
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8761
            initialDelaySeconds: 30
            periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: discovery-server
  namespace: trading-system
spec:
  type: ClusterIP
  ports:
    - port: 8761
      targetPort: 8761
  selector:
    app: discovery-server
```

---

## 5. Autoscaling Configuration

### 5.1 Horizontal Pod Autoscaler (HPA) - Standard Metrics

**File**: `k8s/autoscaling/price-service-hpa.yaml`

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: price-service-hpa
  namespace: trading-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: price-service
  minReplicas: 2
  maxReplicas: 20
  metrics:
    # CPU-based scaling
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70

    # Memory-based scaling
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80

  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 60
        - type: Pods
          value: 4
          periodSeconds: 60
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 25
          periodSeconds: 120
      selectPolicy: Min
```

---

### 5.2 KEDA ScaledObject - Custom WebSocket Metrics

**File**: `k8s/autoscaling/price-service-keda.yaml`

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: price-service-scaledobject
  namespace: trading-system
spec:
  scaleTargetRef:
    name: price-service
  pollingInterval: 15
  cooldownPeriod: 300
  minReplicaCount: 2
  maxReplicaCount: 20
  fallback:
    failureThreshold: 3
    replicas: 5
  triggers:
    # WebSocket connections (custom metric from Prometheus)
    - type: prometheus
      metadata:
        serverAddress: http://prometheus.trading-monitoring.svc.cluster.local:9090
        metricName: websocket_connections_active
        query: sum(websocket_connections_active{service="price-service"}) / count(up{job="price-service"})
        threshold: '400'
        activationThreshold: '100'

    # RabbitMQ queue depth (backpressure indicator)
    - type: rabbitmq
      metadata:
        host: 'amqp://guest:guest@rabbitmq.trading-data.svc.cluster.local:5672/'
        queueName: price.updates
        queueLength: '1000'

    # CPU as fallback
    - type: cpu
      metricType: Utilization
      metadata:
        value: '70'
```

---

### 5.3 KEDA Installation

```bash
# Install KEDA using Helm
helm repo add kedacore https://kedacore.github.io/charts
helm repo update

helm install keda kedacore/keda \
  --namespace keda \
  --create-namespace \
  --set prometheus.metricServer.enabled=true \
  --set prometheus.operator.enabled=false
```

---

## 6. Custom Metrics for WebSocket

### 6.1 Micrometer Configuration in Price Service

**File**: `price-service/src/main/java/org/example/priceservice/config/MetricsConfig.java` (NEW)

```java
package org.example.priceservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Slf4j
public class MetricsConfig {

    private final AtomicInteger activeWebSocketConnections = new AtomicInteger(0);
    private final AtomicInteger activeSubscriptions = new AtomicInteger(0);

    @Bean
    public AtomicInteger activeWebSocketConnections() {
        return activeWebSocketConnections;
    }

    @Bean
    public AtomicInteger activeSubscriptions() {
        return activeSubscriptions;
    }

    @Bean
    public Gauge websocketConnectionsGauge(MeterRegistry registry) {
        return Gauge.builder("websocket_connections_active", activeWebSocketConnections, AtomicInteger::get)
            .description("Number of active WebSocket connections")
            .tag("service", "price-service")
            .register(registry);
    }

    @Bean
    public Gauge websocketSubscriptionsGauge(MeterRegistry registry) {
        return Gauge.builder("websocket_subscriptions_active", activeSubscriptions, AtomicInteger::get)
            .description("Number of active WebSocket subscriptions")
            .tag("service", "price-service")
            .register(registry);
    }

    @Bean
    public Counter messagesPublishedCounter(MeterRegistry registry) {
        return Counter.builder("websocket_messages_published_total")
            .description("Total number of WebSocket messages published")
            .tag("service", "price-service")
            .register(registry);
    }
}
```

---

### 6.2 WebSocket Connection Tracking

**File**: `price-service/src/main/java/org/example/priceservice/config/WebSocketEventListener.java` (NEW)

```java
package org.example.priceservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final AtomicInteger activeWebSocketConnections;
    private final AtomicInteger activeSubscriptions;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        int count = activeWebSocketConnections.incrementAndGet();
        log.debug("WebSocket connected. Active connections: {}", count);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        int count = activeWebSocketConnections.decrementAndGet();
        log.debug("WebSocket disconnected. Active connections: {}", count);
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        int count = activeSubscriptions.incrementAndGet();
        log.debug("Subscribed to {}. Active subscriptions: {}", destination, count);
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        int count = activeSubscriptions.decrementAndGet();
        log.debug("Unsubscribed. Active subscriptions: {}", count);
    }
}
```

---

### 6.3 Prometheus ServiceMonitor

**File**: `k8s/monitoring/price-service-servicemonitor.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: price-service-monitor
  namespace: trading-monitoring
  labels:
    app: price-service
spec:
  selector:
    matchLabels:
      app: price-service
  namespaceSelector:
    matchNames:
      - trading-system
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
      scrapeTimeout: 10s
```

---

## 7. Ingress & Load Balancing

### 7.1 NGINX Ingress Controller Installation

```bash
# Install NGINX Ingress Controller
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm install nginx-ingress ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.replicaCount=2 \
  --set controller.config.proxy-read-timeout="3600" \
  --set controller.config.proxy-send-timeout="3600" \
  --set controller.config.upstream-keepalive-connections="100"
```

---

### 7.2 Ingress Configuration for WebSocket

**File**: `k8s/ingress/trading-ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: trading-ingress
  namespace: trading-system
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: 'true'

    # WebSocket support
    nginx.ingress.kubernetes.io/proxy-read-timeout: '3600'
    nginx.ingress.kubernetes.io/proxy-send-timeout: '3600'
    nginx.ingress.kubernetes.io/websocket-services: 'price-service,api-gateway'

    # Sticky sessions for WebSocket affinity
    nginx.ingress.kubernetes.io/affinity: 'cookie'
    nginx.ingress.kubernetes.io/session-cookie-name: 'TRADING_AFFINITY'
    nginx.ingress.kubernetes.io/session-cookie-expires: '172800'
    nginx.ingress.kubernetes.io/session-cookie-max-age: '172800'
    nginx.ingress.kubernetes.io/session-cookie-path: '/'
    nginx.ingress.kubernetes.io/session-cookie-samesite: 'Strict'

    # Connection limits
    nginx.ingress.kubernetes.io/limit-connections: '50'
    nginx.ingress.kubernetes.io/limit-rps: '100'

    # Enable CORS
    nginx.ingress.kubernetes.io/enable-cors: 'true'
    nginx.ingress.kubernetes.io/cors-allow-origin: 'https://trading.example.com'
    nginx.ingress.kubernetes.io/cors-allow-methods: 'GET, POST, PUT, DELETE, OPTIONS'
    nginx.ingress.kubernetes.io/cors-allow-headers: 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization'
spec:
  tls:
    - hosts:
        - api.trading.example.com
      secretName: trading-tls-secret
  rules:
    - host: api.trading.example.com
      http:
        paths:
          # API Gateway routes
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: api-gateway
                port:
                  number: 8081

          # Direct WebSocket to Price Service
          - path: /ws
            pathType: Prefix
            backend:
              service:
                name: price-service
                port:
                  number: 8083

          # Eureka dashboard (internal only in production)
          - path: /eureka
            pathType: Prefix
            backend:
              service:
                name: discovery-server
                port:
                  number: 8761
```

---

### 7.3 TLS Certificate Secret

**File**: `k8s/ingress/tls-secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: trading-tls-secret
  namespace: trading-system
type: kubernetes.io/tls
data:
  # Base64 encoded certificate and key
  # Generate with: cat cert.pem | base64 -w0
  tls.crt: LS0tLS1CRUdJTi... # Your certificate
  tls.key: LS0tLS1CRUdJTi... # Your private key
```

---

## 8. RabbitMQ Integration

### 8.1 RabbitMQ StatefulSet

**File**: `k8s/data/rabbitmq-statefulset.yaml`

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: rabbitmq
  namespace: trading-data
spec:
  serviceName: rabbitmq
  replicas: 3
  selector:
    matchLabels:
      app: rabbitmq
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      containers:
        - name: rabbitmq
          image: rabbitmq:3-management-alpine
          ports:
            - containerPort: 5672
              name: amqp
            - containerPort: 15672
              name: management
            - containerPort: 61613
              name: stomp
          env:
            - name: RABBITMQ_DEFAULT_USER
              value: 'guest'
            - name: RABBITMQ_DEFAULT_PASS
              valueFrom:
                secretKeyRef:
                  name: rabbitmq-secret
                  key: password
            - name: RABBITMQ_ERLANG_COOKIE
              valueFrom:
                secretKeyRef:
                  name: rabbitmq-secret
                  key: erlang-cookie
          command:
            - sh
            - -c
            - |
              rabbitmq-plugins enable --offline rabbitmq_management rabbitmq_stomp rabbitmq_web_stomp
              docker-entrypoint.sh rabbitmq-server
          resources:
            requests:
              cpu: '200m'
              memory: '512Mi'
            limits:
              cpu: '500m'
              memory: '1Gi'
          volumeMounts:
            - name: rabbitmq-data
              mountPath: /var/lib/rabbitmq
          livenessProbe:
            exec:
              command:
                - rabbitmq-diagnostics
                - -q
                - ping
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            exec:
              command:
                - rabbitmq-diagnostics
                - -q
                - check_running
            initialDelaySeconds: 30
            periodSeconds: 10
  volumeClaimTemplates:
    - metadata:
        name: rabbitmq-data
      spec:
        accessModes: ['ReadWriteOnce']
        storageClassName: standard
        resources:
          requests:
            storage: 10Gi

---
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq
  namespace: trading-data
spec:
  type: ClusterIP
  ports:
    - port: 5672
      targetPort: 5672
      name: amqp
    - port: 15672
      targetPort: 15672
      name: management
    - port: 61613
      targetPort: 61613
      name: stomp
  selector:
    app: rabbitmq
```

---

### 8.2 RabbitMQ Cluster Configuration

**File**: `k8s/data/rabbitmq-config.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rabbitmq-config
  namespace: trading-data
data:
  rabbitmq.conf: |
    ## Cluster formation
    cluster_formation.peer_discovery_backend = rabbit_peer_discovery_k8s
    cluster_formation.k8s.host = kubernetes.default.svc.cluster.local
    cluster_formation.k8s.address_type = hostname
    cluster_formation.node_cleanup.interval = 30
    cluster_formation.node_cleanup.only_log_warning = true
    cluster_partition_handling = autoheal

    ## Memory and connection limits
    vm_memory_high_watermark.relative = 0.7
    vm_memory_high_watermark_paging_ratio = 0.75

    ## STOMP configuration
    stomp.default_user = guest
    stomp.default_pass = guest

    ## Management
    management.tcp.port = 15672

  enabled_plugins: |
    [rabbitmq_management,rabbitmq_peer_discovery_k8s,rabbitmq_stomp,rabbitmq_web_stomp].
```

---

## 9. Complete Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                                   │
│                           TRADING PLATFORM - COMPLETE KUBERNETES ARCHITECTURE                      │
│                                                                                                   │
├───────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│   │                                    EXTERNAL SERVICES                                         │ │
│   │                                                                                              │ │
│   │   ┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐                   │ │
│   │   │   Binance API    │     │  MongoDB Atlas   │     │     Firebase     │                   │ │
│   │   │   (WebSocket +   │     │   (User Data)    │     │   (Email SMTP)   │                   │ │
│   │   │    REST API)     │     │                  │     │                  │                   │ │
│   │   └────────┬─────────┘     └────────┬─────────┘     └────────┬─────────┘                   │ │
│   │            │                        │                        │                             │ │
│   └────────────┼────────────────────────┼────────────────────────┼─────────────────────────────┘ │
│                │                        │                        │                               │
│   ═════════════╪════════════════════════╪════════════════════════╪═══════════════════════════   │
│                │            KUBERNETES CLUSTER BOUNDARY          │                               │
│   ═════════════╪════════════════════════╪════════════════════════╪═══════════════════════════   │
│                │                        │                        │                               │
│   ┌────────────┼────────────────────────┼────────────────────────┼─────────────────────────────┐ │
│   │            │              INGRESS LAYER                      │                             │ │
│   │            │                                                 │                             │ │
│   │   ┌────────┼─────────────────────────────────────────────────┼───────────────────────────┐ │ │
│   │   │        │            NGINX INGRESS CONTROLLER             │                           │ │ │
│   │   │        │   ┌─────────────────────────────────────────────┼───────────────────────┐   │ │ │
│   │   │        │   │  • TLS Termination (HTTPS/WSS)              │                       │   │ │ │
│   │   │        │   │  • Sticky Sessions (Cookie-based)           │                       │   │ │ │
│   │   │        │   │  • Rate Limiting (100 req/s)                │                       │   │ │ │
│   │   │        │   │  • WebSocket Upgrade Support                │                       │   │ │ │
│   │   │        │   │  • Connection Timeout: 3600s                │                       │   │ │ │
│   │   │        │   └─────────────────────────────────────────────┼───────────────────────┘   │ │ │
│   │   │        │                                                 │                           │ │ │
│   │   │        │   Routes:                                       │                           │ │ │
│   │   │        │   /api/*  → api-gateway:8081                    │                           │ │ │
│   │   │        │   /ws/*   → price-service:8083 (direct WS)      │                           │ │ │
│   │   │        │                                                 │                           │ │ │
│   │   └────────┼─────────────────────────────────────────────────┼───────────────────────────┘ │ │
│   │            │                                                 │                             │ │
│   └────────────┼─────────────────────────────────────────────────┼─────────────────────────────┘ │
│                │                                                 │                               │
│   ┌────────────┼─────────────────────────────────────────────────┼─────────────────────────────┐ │
│   │            │        NAMESPACE: trading-system                │                             │ │
│   │            │                                                 │                             │ │
│   │   ┌────────┼───────────────────────────────────┐             │                             │ │
│   │   │        │       SERVICE DISCOVERY           │             │                             │ │
│   │   │        │                                   │             │                             │ │
│   │   │   ┌────┴────────────────────────────┐     │             │                             │ │
│   │   │   │      DISCOVERY SERVER           │     │             │                             │ │
│   │   │   │      (Eureka) :8761             │     │             │                             │ │
│   │   │   │      Replicas: 1                │     │             │                             │ │
│   │   │   └────┬────────────────────────────┘     │             │                             │ │
│   │   │        │ Service Registration             │             │                             │ │
│   │   └────────┼──────────────────────────────────┘             │                             │ │
│   │            │                                                 │                             │ │
│   │   ┌────────┼──────────────────────────────────────────────────────────────────────────┐   │ │
│   │   │        │                    MICROSERVICES                                          │   │ │
│   │   │        ▼                                                                           │   │ │
│   │   │   ┌──────────────┐    ┌──────────────────────┐    ┌──────────────┐               │   │ │
│   │   │   │ API GATEWAY  │    │    PRICE SERVICE     │    │ USER SERVICE │               │   │ │
│   │   │   │   :8081      │    │       :8083          │    │    :8082     │               │   │ │
│   │   │   │              │    │                      │    │              │               │   │ │
│   │   │   │ Replicas: 2  │    │  ┌────────────────┐  │    │ Replicas: 2  │               │   │ │
│   │   │   │              │    │  │ HPA: 2-20 pods │  │    │              │               │   │ │
│   │   │   │ • Routing    │    │  │                │  │    │ • Auth       │               │   │ │
│   │   │   │ • Load Bal.  │    │  │ Scale Triggers:│  │    │ • User CRUD  │               │   │ │
│   │   │   │ • Circuit    │    │  │ • CPU > 70%    │  │    │ • JWT Token  │               │   │ │
│   │   │   │   Breaker    │    │  │ • Memory > 80% │  │    │ • Roles      │               │   │ │
│   │   │   │              │    │  │ • WS Conn >400 │  │    │              │◄──────────────┼───┼─┤
│   │   │   └──────┬───────┘    │  └────────────────┘  │    └──────┬───────┘   MongoDB    │   │ │
│   │   │          │            │                      │           │           Atlas      │   │ │
│   │   │          │            │  Features:           │           │                      │   │ │
│   │   │          │            │  • WebSocket Server  │           │                      │   │ │
│   │   │          │            │  • STOMP Protocol    │           │                      │   │ │
│   │   │          │            │  • Price Streaming   │           │                      │   │ │
│   │   │          │            │  • Multi-symbol      │           │                      │   │ │
│   │   │          │            │  • Multi-timeframe   │           │                      │   │ │
│   │   │          │            └──────────┬───────────┘           │                      │   │ │
│   │   │          │                       │                       │                      │   │ │
│   │   └──────────┼───────────────────────┼───────────────────────┼──────────────────────┘   │ │
│   │              │                       │                       │                          │ │
│   │   ┌──────────┼───────────────────────┼───────────────────────┼──────────────────────┐   │ │
│   │   │          │        DATA COLLECTORS                        │                      │   │ │
│   │   │          │                       │                       │                      │   │ │
│   │   │   ┌──────┴───────┐               │                       │                      │   │ │
│   │   │   │    PRICE     │               │                       │                      │   │ │
│   │   │   │  COLLECTOR   │               │                       │                      │   │ │
│   │   │   │   :8083      │               │                       │                      │   │ │
│   │   │   │              │               │                       │                      │   │ │
│   │   │   │ Replicas: 1  │               │                       │                      │   │ │
│   │   │   │ (Singleton)  │               │                       │                      │   │ │
│   │   │   │              │               │                       │                      │   │ │
│   │   │   │ • Binance WS │               │                       │                      │   │ │
│   │   │   │ • Kline Data │               │                       │                      │   │ │
│   │   │   │ • Publisher  │               │                       │                      │   │ │
│   │   │   │              │               │                       │                      │   │ │
│   │   │   └──────┬───────┘               │                       │                      │   │ │
│   │   │          │                       │                       │                      │   │ │
│   │   └──────────┼───────────────────────┼───────────────────────┼──────────────────────┘   │ │
│   │              │                       │                       │                          │ │
│   │              │                       │                       │                          │ │
│   └──────────────┼───────────────────────┼───────────────────────┼──────────────────────────┘ │
│                  │                       │                       │                            │
│   ┌──────────────┼───────────────────────┼───────────────────────┼──────────────────────────┐ │
│   │              │        NAMESPACE: trading-data                │                          │ │
│   │              │                       │                       │                          │ │
│   │   ┌──────────┼───────────────────────┼───────────────────────┼──────────────────────┐   │ │
│   │   │          │           MESSAGE BROKER                      │                      │   │ │
│   │   │          │                       │                       │                      │   │ │
│   │   │          │           ┌───────────▼───────────┐           │                      │   │ │
│   │   │          │           │      RABBITMQ         │           │                      │   │ │
│   │   │          │           │   (StatefulSet x 3)   │           │                      │   │ │
│   │   │          │           │                       │           │                      │   │ │
│   │   │          │           │  • AMQP :5672         │           │                      │   │ │
│   │   │          │           │  • STOMP :61613       │           │                      │   │ │
│   │   │          │           │  • Management :15672  │           │                      │   │ │
│   │   │          │           │                       │           │                      │   │ │
│   │   │          └──────────▶│  Exchanges:           │◀──────────┘                      │   │ │
│   │   │    (Publish)         │  • price.exchange     │     (Subscribe)                  │   │ │
│   │   │                      │  • news.exchange      │                                  │   │ │
│   │   │                      │  • ai.exchange        │                                  │   │ │
│   │   │                      │                       │                                  │   │ │
│   │   │                      └───────────────────────┘                                  │   │ │
│   │   │                                                                                 │   │ │
│   │   └─────────────────────────────────────────────────────────────────────────────────┘   │ │
│   │                                                                                         │ │
│   │   ┌─────────────────────────────────────────────────────────────────────────────────┐   │ │
│   │   │                              DATABASES                                           │   │ │
│   │   │                                                                                  │   │ │
│   │   │   ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐         │   │ │
│   │   │   │   POSTGRESQL     │    │      REDIS       │    │   (External)     │         │   │ │
│   │   │   │  (StatefulSet)   │    │  (StatefulSet)   │    │  MONGODB ATLAS   │         │   │ │
│   │   │   │                  │    │                  │    │                  │         │   │ │
│   │   │   │  • Price Data    │    │  • Session Cache │    │  • User Data     │         │   │ │
│   │   │   │  • Candles       │    │  • Rate Limiting │    │  • Refresh Token │         │   │ │
│   │   │   │  • Ticks         │    │  • WS State      │    │  • Articles      │         │   │ │
│   │   │   │                  │    │                  │    │                  │         │   │ │
│   │   │   │  :5432           │    │  :6379           │    │  Cloud Hosted    │         │   │ │
│   │   │   │  Storage: 50Gi   │    │  Storage: 10Gi   │    │                  │         │   │ │
│   │   │   │                  │    │                  │    │                  │         │   │ │
│   │   │   └──────────────────┘    └──────────────────┘    └──────────────────┘         │   │ │
│   │   │                                                                                  │   │ │
│   │   └─────────────────────────────────────────────────────────────────────────────────┘   │ │
│   │                                                                                         │ │
│   └─────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                               │
│   ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │
│   │                    NAMESPACE: trading-monitoring                                         │ │
│   │                                                                                          │ │
│   │   ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐                 │ │
│   │   │    PROMETHEUS    │    │     GRAFANA      │    │      KEDA        │                 │ │
│   │   │                  │    │                  │    │                  │                 │ │
│   │   │  • Metrics       │───▶│  • Dashboards    │    │  • ScaledObject  │                 │ │
│   │   │  • ServiceMonitor│    │  • Alerts        │    │  • Custom Metrics│                 │ │
│   │   │  • Custom Metrics│    │  • Visualize     │    │  • Autoscaling   │                 │ │
│   │   │                  │    │                  │    │                  │                 │ │
│   │   │  :9090           │    │  :3000           │    │                  │                 │ │
│   │   │                  │    │                  │    │                  │                 │ │
│   │   └──────────────────┘    └──────────────────┘    └──────────────────┘                 │ │
│   │                                                                                          │ │
│   └─────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                               │
├───────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                               │
│   CLIENTS (EXTERNAL)                                                                          │
│                                                                                               │
│   ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐                       │
│   │   WEB BROWSER    │    │   MOBILE APP     │    │    API CLIENT    │                       │
│   │   (Next.js)      │    │                  │    │                  │                       │
│   │                  │    │                  │    │                  │                       │
│   │  • React UI      │    │  • iOS/Android   │    │  • REST API      │                       │
│   │  • WebSocket     │    │  • WebSocket     │    │  • Automation    │                       │
│   │  • Real-time     │    │  • Push Notif.   │    │                  │                       │
│   │                  │    │                  │    │                  │                       │
│   └──────────────────┘    └──────────────────┘    └──────────────────┘                       │
│                                                                                               │
└───────────────────────────────────────────────────────────────────────────────────────────────┘

                                     DATA FLOW LEGEND
    ═══════════════════════════════════════════════════════════════════════════════

    ────────▶  HTTP/REST API Request
    ════════▶  WebSocket Connection (Persistent)
    - - - - ▶  Pub/Sub Message (RabbitMQ)
    ◀───────▶  Bidirectional Communication

    SCALING BEHAVIOR:
    ┌─────────────────────────────────────────────────────────────────────────────┐
    │                                                                             │
    │   TRIGGER                    ACTION                    COOLDOWN             │
    │   ───────                    ──────                    ────────             │
    │   CPU > 70%          ───▶   Scale Up +25%       ───▶   60 seconds          │
    │   Memory > 80%       ───▶   Scale Up +25%       ───▶   60 seconds          │
    │   WS Connections >400───▶   Scale Up +1 pod    ───▶   60 seconds          │
    │   CPU < 30%          ───▶   Scale Down -25%    ───▶   300 seconds         │
    │   WS Connections <100───▶   Scale Down -1 pod  ───▶   300 seconds         │
    │                                                                             │
    │   Min Replicas: 2                                                           │
    │   Max Replicas: 20                                                          │
    │                                                                             │
    └─────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Implementation Steps

### Phase 1: Infrastructure Setup (Week 1)

| Day | Task                           | Details                                          |
| --- | ------------------------------ | ------------------------------------------------ |
| 1   | Create Kubernetes Cluster      | GKE/EKS/AKS with 3 nodes                         |
| 1   | Install NGINX Ingress          | Helm chart deployment                            |
| 2   | Create Namespaces              | trading-system, trading-data, trading-monitoring |
| 2   | Deploy ConfigMaps/Secrets      | Environment configuration                        |
| 3   | Deploy RabbitMQ                | StatefulSet with persistence                     |
| 3   | Deploy Redis                   | StatefulSet with persistence                     |
| 4   | Deploy PostgreSQL              | StatefulSet with persistence                     |
| 4   | Verify data layer connectivity | Health checks                                    |
| 5   | Deploy Discovery Server        | Eureka service registry                          |

### Phase 2: Application Deployment (Week 2)

| Day | Task                       | Details                             |
| --- | -------------------------- | ----------------------------------- |
| 1   | Build Docker images        | Multi-stage Dockerfile optimization |
| 1   | Push to Container Registry | GCR/ECR/ACR                         |
| 2   | Deploy User Service        | With MongoDB Atlas connection       |
| 2   | Deploy Price Collector     | Singleton deployment                |
| 3   | Deploy Price Service       | Initial 2 replicas                  |
| 3   | Deploy API Gateway         | Load balanced                       |
| 4   | Configure Ingress          | TLS, sticky sessions                |
| 4   | End-to-end testing         | WebSocket connectivity              |
| 5   | Bug fixes and optimization | Performance tuning                  |

### Phase 3: Autoscaling Setup (Week 3)

| Day | Task                        | Details                       |
| --- | --------------------------- | ----------------------------- |
| 1   | Add Micrometer metrics      | WebSocket connection tracking |
| 1   | Deploy Prometheus           | ServiceMonitor configuration  |
| 2   | Deploy Grafana              | Dashboards setup              |
| 2   | Install KEDA                | Custom metrics scaler         |
| 3   | Configure HPA               | CPU/Memory targets            |
| 3   | Configure KEDA ScaledObject | WebSocket metrics             |
| 4   | Load testing                | Simulate 1000+ connections    |
| 4   | Tune autoscaling parameters | Adjust thresholds             |
| 5   | Documentation               | Runbook creation              |

---

## 11. Monitoring & Observability

### 11.1 Grafana Dashboard JSON

**File**: `k8s/monitoring/grafana-dashboard.json`

```json
{
  "dashboard": {
    "title": "Trading Platform - Price Service",
    "panels": [
      {
        "title": "Active WebSocket Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(websocket_connections_active{service=\"price-service\"})",
            "legendFormat": "Total Connections"
          },
          {
            "expr": "avg(websocket_connections_active{service=\"price-service\"})",
            "legendFormat": "Avg per Pod"
          }
        ]
      },
      {
        "title": "Pod Count",
        "type": "stat",
        "targets": [
          {
            "expr": "count(up{job=\"price-service\"})"
          }
        ]
      },
      {
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "avg(rate(container_cpu_usage_seconds_total{pod=~\"price-service.*\"}[5m])) * 100",
            "legendFormat": "CPU %"
          }
        ]
      },
      {
        "title": "Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "avg(container_memory_usage_bytes{pod=~\"price-service.*\"}) / 1024 / 1024",
            "legendFormat": "Memory MB"
          }
        ]
      },
      {
        "title": "Messages Published/sec",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(websocket_messages_published_total{service=\"price-service\"}[1m])",
            "legendFormat": "Messages/sec"
          }
        ]
      }
    ]
  }
}
```

---

### 11.2 Alerting Rules

**File**: `k8s/monitoring/prometheus-rules.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: price-service-alerts
  namespace: trading-monitoring
spec:
  groups:
    - name: price-service
      rules:
        - alert: HighWebSocketConnections
          expr: sum(websocket_connections_active{service="price-service"}) > 8000
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: 'High WebSocket connection count'
            description: 'WebSocket connections above 8000 for 5 minutes'

        - alert: PriceServiceDown
          expr: up{job="price-service"} == 0
          for: 1m
          labels:
            severity: critical
          annotations:
            summary: 'Price Service instance down'
            description: 'Price Service pod {{ $labels.pod }} is down'

        - alert: HighCPUUsage
          expr: avg(rate(container_cpu_usage_seconds_total{pod=~"price-service.*"}[5m])) > 0.8
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: 'High CPU usage on Price Service'
            description: 'CPU usage above 80% for 10 minutes'

        - alert: RabbitMQQueueBacklog
          expr: rabbitmq_queue_messages{queue="price.updates"} > 5000
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: 'RabbitMQ queue backlog growing'
            description: 'Price updates queue has more than 5000 messages'
```

---

## 12. Disaster Recovery

### 12.1 Backup Strategy

| Component     | Backup Method      | Frequency  | Retention |
| ------------- | ------------------ | ---------- | --------- |
| PostgreSQL    | pg_dump + S3       | Daily      | 30 days   |
| Redis         | RDB snapshot       | Hourly     | 7 days    |
| RabbitMQ      | Definitions export | Daily      | 30 days   |
| MongoDB Atlas | Automated backups  | Continuous | 7 days    |

### 12.2 Recovery Procedures

**Price Service Pod Failure**:

1. Kubernetes auto-restarts failed pod
2. New pod registers with Eureka
3. Clients reconnect via sticky session failover
4. KEDA scales if needed

**Complete Cluster Failure**:

1. Provision new cluster from IaC (Terraform)
2. Apply Kubernetes manifests
3. Restore databases from backups
4. Verify service health
5. Update DNS if needed

---

## 13. Cost Estimation

### 13.1 Resource Requirements

| Service          | Min Replicas | Max Replicas | CPU Request | Memory Request |
| ---------------- | ------------ | ------------ | ----------- | -------------- |
| price-service    | 2            | 20           | 250m        | 512Mi          |
| price-collector  | 1            | 1            | 100m        | 256Mi          |
| user-service     | 2            | 2            | 200m        | 384Mi          |
| api-gateway      | 2            | 2            | 200m        | 384Mi          |
| discovery-server | 1            | 1            | 100m        | 256Mi          |
| RabbitMQ         | 3            | 3            | 200m        | 512Mi          |
| Redis            | 3            | 3            | 100m        | 256Mi          |
| PostgreSQL       | 1            | 1            | 200m        | 512Mi          |

### 13.2 Estimated Monthly Cost (GKE)

| Component                 | Specification | Monthly Cost    |
| ------------------------- | ------------- | --------------- |
| GKE Cluster               | 1 cluster     | $72             |
| Node Pool (e2-standard-4) | 3 nodes       | $291            |
| Load Balancer             | 1             | $18             |
| Persistent Disks          | 80GB          | $8              |
| Egress                    | 100GB         | $12             |
| **Total**                 |               | **~$400/month** |

---

## Summary

This document provides a comprehensive plan to migrate the Trading Platform's price-service from Docker Compose to Kubernetes with autoscaling capabilities.

### Key Deliverables

1. ✅ **Kubernetes Manifests** - Complete deployment configurations
2. ✅ **HPA + KEDA** - Autoscaling based on WebSocket metrics
3. ✅ **Ingress Configuration** - WebSocket-optimized NGINX setup
4. ✅ **Monitoring Stack** - Prometheus, Grafana, alerting rules
5. ✅ **Complete Architecture Diagram** - Visual representation

### Next Steps

1. Review and approve architecture design
2. Set up Kubernetes cluster
3. Begin Phase 1 implementation
4. Conduct load testing
5. Production deployment

---

## References

1. Kubernetes Documentation: https://kubernetes.io/docs/
2. KEDA Documentation: https://keda.sh/docs/
3. NGINX Ingress for WebSocket: https://kubernetes.github.io/ingress-nginx/user-guide/miscellaneous/#websockets
4. RabbitMQ on Kubernetes: https://www.rabbitmq.com/kubernetes/operator/operator-overview.html
5. Prometheus Operator: https://prometheus-operator.dev/
