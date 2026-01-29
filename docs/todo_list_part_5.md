# Trading Platform Backend - TODO Part 5: Firebase Integration & MongoDB Atlas Migration

**Document Version**: 5.0  
**Date**: January 26, 2026  
**Prerequisites**: [answer.md](./answer.md) - Architecture Q&A Document  
**Focus**: Firebase Email Service, MongoDB Atlas Migration, Service Integration Points

---

## Executive Summary

### Previous Status (from Part 4)

**✅ COMPLETED**:

- User Subscription System (REGULAR/VIP)
- Password Recovery Flow (mock email)
- Role-Based Access Control
- JWT with roles
- User Profile endpoints
- Price Service with WebSocket
- Price Collector Service
- RabbitMQ integration

**🎯 THIS DOCUMENT FOCUSES ON**:

1. Firebase Admin SDK Integration for Email Service
2. MongoDB Atlas Migration for User Service
3. Integration points for News/Crawler/AI services (teammate handoff)
4. Production readiness improvements

**⚠️ OUT OF SCOPE** (handled by teammate):

- News Service implementation
- Crawler Service implementation
- AI Analysis Service implementation

---

## Table of Contents

1. [Firebase Email Service Integration](#1-firebase-email-service-integration)
2. [MongoDB Atlas Migration Plan](#2-mongodb-atlas-migration-plan)
3. [Service Integration Points](#3-service-integration-points-for-teammate)
4. [Production Readiness Tasks](#4-production-readiness-tasks)
5. [Implementation Priority Matrix](#5-implementation-priority-matrix)
6. [Timeline Estimation](#6-timeline-estimation)

---

## 1. Firebase Email Service Integration

### 1.1 Current State Analysis

**Current Implementation** (`user-service/src/main/java/org/example/userservice/service/EmailService.java`):

- Mock implementation that logs emails instead of sending
- No actual email delivery capability
- Password reset tokens generated but not delivered

### 1.2 Firebase Setup Tasks

#### Task FE-1: Create Firebase Project

**Priority**: 🔴 HIGH  
**Estimated Time**: 30 minutes

**Steps**:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create new project: `trading-platform-prod`
3. Enable Authentication (for future OAuth integration)
4. Generate Service Account Key:
   - Project Settings → Service Accounts → Generate New Private Key
   - Save as `firebase-service-account.json`

**Expected Output**:

```json
{
  "type": "service_account",
  "project_id": "trading-platform-prod",
  "private_key_id": "xxx",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...",
  "client_email": "firebase-adminsdk-xxx@trading-platform-prod.iam.gserviceaccount.com",
  "client_id": "xxx",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token"
}
```

---

#### Task FE-2: Add Firebase Admin SDK Dependencies

**Priority**: 🔴 HIGH  
**Estimated Time**: 15 minutes

**File**: `user-service/pom.xml`

```xml
<!-- Add to dependencies section -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>

<!-- For Gmail API via Firebase -->
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-gmail</artifactId>
    <version>v1-rev20231218-2.0.0</version>
</dependency>

<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.23.0</version>
</dependency>
```

---

#### Task FE-3: Create Firebase Configuration

**Priority**: 🔴 HIGH  
**Estimated Time**: 30 minutes

**File**: `user-service/src/main/java/org/example/userservice/config/FirebaseConfig.java` (NEW)

```java
package org.example.userservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials.path:classpath:firebase-service-account.json}")
    private Resource firebaseCredentials;

    @Value("${firebase.project-id:trading-platform-prod}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = firebaseCredentials.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully for project: {}", projectId);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}
```

---

#### Task FE-4: Implement Gmail Email Service

**Priority**: 🔴 HIGH  
**Estimated Time**: 1 hour

**Note**: Firebase Admin SDK doesn't directly support email sending. We'll use Gmail API with service account or SMTP.

**Option A: Gmail SMTP (Recommended for simplicity)**

**File**: `user-service/src/main/java/org/example/userservice/service/GmailEmailService.java` (NEW)

```java
package org.example.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
@RequiredArgsConstructor
public class GmailEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Trading Platform - Password Reset Request");

            Context context = new Context();
            context.setVariable("resetLink", frontendUrl + "/reset-password?token=" + resetToken);
            context.setVariable("expirationMinutes", 30);

            String htmlContent = templateEngine.process("email/password-reset", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Trading Platform!");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("loginUrl", frontendUrl + "/login");

            String htmlContent = templateEngine.process("email/welcome", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    @Override
    @Async
    public void sendSubscriptionUpgradeEmail(String toEmail, String userName, String newTier) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Trading Platform - Subscription Upgraded to " + newTier);

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("newTier", newTier);
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

            String htmlContent = templateEngine.process("email/subscription-upgrade", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Subscription upgrade email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send subscription upgrade email to: {}", toEmail, e);
        }
    }
}
```

---

#### Task FE-5: Add Email Templates

**Priority**: 🟡 MEDIUM  
**Estimated Time**: 45 minutes

**File**: `user-service/src/main/resources/templates/email/password-reset.html` (NEW)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
  <head>
    <meta charset="UTF-8" />
    <title>Password Reset</title>
    <style>
      body {
        font-family: Arial, sans-serif;
        line-height: 1.6;
        color: #333;
      }
      .container {
        max-width: 600px;
        margin: 0 auto;
        padding: 20px;
      }
      .header {
        background: #1a1a2e;
        color: white;
        padding: 20px;
        text-align: center;
      }
      .content {
        padding: 30px;
        background: #f9f9f9;
      }
      .button {
        display: inline-block;
        padding: 12px 30px;
        background: #4caf50;
        color: white;
        text-decoration: none;
        border-radius: 5px;
        margin: 20px 0;
      }
      .footer {
        text-align: center;
        padding: 20px;
        color: #666;
        font-size: 12px;
      }
    </style>
  </head>
  <body>
    <div class="container">
      <div class="header">
        <h1>🔐 Password Reset Request</h1>
      </div>
      <div class="content">
        <p>Hello,</p>
        <p>
          We received a request to reset your password for your Trading Platform
          account.
        </p>
        <p>Click the button below to reset your password:</p>
        <p style="text-align: center;">
          <a th:href="${resetLink}" class="button">Reset Password</a>
        </p>
        <p>
          This link will expire in
          <strong th:text="${expirationMinutes}">30</strong> minutes.
        </p>
        <p>
          If you didn't request this, please ignore this email or contact
          support if you have concerns.
        </p>
      </div>
      <div class="footer">
        <p>© 2026 Trading Platform. All rights reserved.</p>
        <p>This is an automated message, please do not reply.</p>
      </div>
    </div>
  </body>
</html>
```

**File**: `user-service/src/main/resources/templates/email/welcome.html` (NEW)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
  <head>
    <meta charset="UTF-8" />
    <title>Welcome</title>
    <style>
      body {
        font-family: Arial, sans-serif;
        line-height: 1.6;
        color: #333;
      }
      .container {
        max-width: 600px;
        margin: 0 auto;
        padding: 20px;
      }
      .header {
        background: #1a1a2e;
        color: white;
        padding: 20px;
        text-align: center;
      }
      .content {
        padding: 30px;
        background: #f9f9f9;
      }
      .button {
        display: inline-block;
        padding: 12px 30px;
        background: #2196f3;
        color: white;
        text-decoration: none;
        border-radius: 5px;
      }
      .features {
        margin: 20px 0;
      }
      .features li {
        margin: 10px 0;
      }
      .footer {
        text-align: center;
        padding: 20px;
        color: #666;
        font-size: 12px;
      }
    </style>
  </head>
  <body>
    <div class="container">
      <div class="header">
        <h1>🎉 Welcome to Trading Platform!</h1>
      </div>
      <div class="content">
        <p>Hello <strong th:text="${userName}">User</strong>,</p>
        <p>
          Thank you for joining Trading Platform! Your account has been created
          successfully.
        </p>

        <h3>What you can do:</h3>
        <ul class="features">
          <li>📊 View real-time price charts for multiple currency pairs</li>
          <li>📰 Read the latest financial news and analysis</li>
          <li>⚡ Receive instant price updates via WebSocket</li>
        </ul>

        <p style="text-align: center;">
          <a th:href="${loginUrl}" class="button">Go to Dashboard</a>
        </p>

        <p>
          Upgrade to VIP to unlock AI-powered market analysis and predictions!
        </p>
      </div>
      <div class="footer">
        <p>© 2026 Trading Platform. All rights reserved.</p>
      </div>
    </div>
  </body>
</html>
```

---

#### Task FE-6: Configure Gmail SMTP

**Priority**: 🔴 HIGH  
**Estimated Time**: 20 minutes

**File**: `user-service/src/main/resources/application.yml` (UPDATE)

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${GMAIL_USERNAME}
    password: ${GMAIL_APP_PASSWORD} # Use App Password, not regular password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
    test-connection: true

  # Thymeleaf for email templates
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML
    encoding: UTF-8
    cache: true
```

**File**: `.env` (UPDATE)

```bash
# Gmail Configuration
GMAIL_USERNAME=your-trading-platform@gmail.com
GMAIL_APP_PASSWORD=xxxx-xxxx-xxxx-xxxx  # Generate from Google Account > Security > App Passwords

# Firebase
FIREBASE_CREDENTIALS_PATH=classpath:firebase-service-account.json
FIREBASE_PROJECT_ID=trading-platform-prod
```

**Setup Gmail App Password**:

1. Go to Google Account → Security
2. Enable 2-Step Verification (required)
3. Go to App Passwords → Generate new app password
4. Select "Mail" and "Other (Custom name)" → "Trading Platform"
5. Copy the 16-character password

---

#### Task FE-7: Add Thymeleaf Dependency

**Priority**: 🟡 MEDIUM  
**Estimated Time**: 10 minutes

**File**: `user-service/pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

---

#### Task FE-8: Update EmailService Interface

**Priority**: 🟡 MEDIUM  
**Estimated Time**: 15 minutes

**File**: `user-service/src/main/java/org/example/userservice/service/EmailService.java` (UPDATE)

```java
package org.example.userservice.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendWelcomeEmail(String toEmail, String userName);
    void sendSubscriptionUpgradeEmail(String toEmail, String userName, String newTier);
}
```

---

### 1.3 Firebase Email Tasks Summary

| Task ID | Task Name                     | Priority  | Time Est. | Dependencies |
| ------- | ----------------------------- | --------- | --------- | ------------ |
| FE-1    | Create Firebase Project       | 🔴 HIGH   | 30 min    | None         |
| FE-2    | Add Firebase Dependencies     | 🔴 HIGH   | 15 min    | FE-1         |
| FE-3    | Firebase Configuration        | 🔴 HIGH   | 30 min    | FE-2         |
| FE-4    | Gmail Email Service           | 🔴 HIGH   | 1 hour    | FE-3         |
| FE-5    | Email Templates               | 🟡 MEDIUM | 45 min    | FE-4         |
| FE-6    | Gmail SMTP Config             | 🔴 HIGH   | 20 min    | FE-4         |
| FE-7    | Thymeleaf Dependency          | 🟡 MEDIUM | 10 min    | None         |
| FE-8    | Update EmailService Interface | 🟡 MEDIUM | 15 min    | FE-4         |

**Total Estimated Time**: ~4 hours

---

## 2. MongoDB Atlas Migration Plan

### 2.1 Current State

**Current Setup**:

- Local MongoDB container (docker-compose.yml)
- Port: 27017
- Credentials: admin/admin123
- Databases: trading_users (User, RefreshToken collections)

### 2.2 Migration Tasks

#### Task MA-1: Create MongoDB Atlas Cluster

**Priority**: 🔴 HIGH  
**Estimated Time**: 45 minutes

**Steps**:

1. Go to [MongoDB Atlas](https://cloud.mongodb.com/)
2. Create new project: `trading-platform`
3. Create cluster:
   - **Tier**: M0 (Free) for dev, M10+ for production
   - **Region**: Select closest to your users
   - **Cluster Name**: `trading-cluster`

4. Configure Network Access:
   - Add IP whitelist for development: `0.0.0.0/0` (temporary)
   - For production: Add specific IPs or VPC peering

5. Create Database User:
   - Username: `trading_app`
   - Password: Generate secure password
   - Roles: `readWriteAnyDatabase`

6. Get Connection String:
   ```
   mongodb+srv://trading_app:<password>@trading-cluster.xxxxx.mongodb.net/trading_users?retryWrites=true&w=majority
   ```

---

#### Task MA-2: Update User Service Configuration

**Priority**: 🔴 HIGH  
**Estimated Time**: 30 minutes

**File**: `user-service/src/main/resources/application.yml` (UPDATE)

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://admin:admin123@localhost:27017/trading_users?authSource=admin}
      database: trading_users
      auto-index-creation: true


# Profile-specific configuration
---
spring:
  config:
    activate:
      on-profile: production
  data:
    mongodb:
      uri: ${MONGODB_ATLAS_URI}
      database: trading_users
```

**File**: `.env.production` (NEW)

```bash
# MongoDB Atlas Production
MONGODB_ATLAS_URI=mongodb+srv://trading_app:YOUR_SECURE_PASSWORD@trading-cluster.xxxxx.mongodb.net/trading_users?retryWrites=true&w=majority&appName=trading-platform
```

---

#### Task MA-3: Data Migration Script

**Priority**: 🔴 HIGH  
**Estimated Time**: 1 hour

**File**: `user-service/scripts/migrate-to-atlas.sh` (NEW)

```bash
#!/bin/bash

# MongoDB Atlas Migration Script
# Run this to migrate data from local MongoDB to Atlas

# Configuration
LOCAL_MONGO_URI="mongodb://admin:admin123@localhost:27017"
ATLAS_URI="mongodb+srv://trading_app:PASSWORD@trading-cluster.xxxxx.mongodb.net"
DATABASE="trading_users"

echo "=== MongoDB Atlas Migration ==="
echo "Source: Local MongoDB"
echo "Target: MongoDB Atlas"
echo ""

# Export from local
echo "Step 1: Exporting data from local MongoDB..."
mongodump --uri="$LOCAL_MONGO_URI" --db=$DATABASE --out=./dump --authenticationDatabase=admin

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to export from local MongoDB"
    exit 1
fi

echo "Step 2: Importing data to MongoDB Atlas..."
mongorestore --uri="$ATLAS_URI" --db=$DATABASE ./dump/$DATABASE --drop

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to import to MongoDB Atlas"
    exit 1
fi

echo ""
echo "=== Migration Complete ==="
echo "Verify data in Atlas console: https://cloud.mongodb.com"

# Cleanup
rm -rf ./dump
```

---

#### Task MA-4: Create MongoDB Atlas Indexes

**Priority**: 🟡 MEDIUM  
**Estimated Time**: 30 minutes

**File**: `user-service/src/main/java/org/example/userservice/config/MongoIndexConfig.java` (NEW)

```java
package org.example.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndexes() {
        log.info("Creating MongoDB indexes...");

        // User collection indexes
        IndexOperations userIndexOps = mongoTemplate.indexOps("users");
        userIndexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
        userIndexOps.ensureIndex(new Index().on("username", Sort.Direction.ASC).unique());
        userIndexOps.ensureIndex(new Index().on("subscriptionTier", Sort.Direction.ASC));

        // RefreshToken collection indexes
        IndexOperations tokenIndexOps = mongoTemplate.indexOps("refresh_tokens");
        tokenIndexOps.ensureIndex(new Index().on("token", Sort.Direction.ASC).unique());
        tokenIndexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC));
        tokenIndexOps.ensureIndex(
            new Index()
                .on("expiryDate", Sort.Direction.ASC)
                .expire(Duration.ofDays(7))  // TTL index
        );

        log.info("MongoDB indexes created successfully");
    }
}
```

---

#### Task MA-5: Update Docker Compose for Atlas

**Priority**: 🟡 MEDIUM  
**Estimated Time**: 20 minutes

**File**: `docker-compose.yml` (UPDATE)

```yaml
# Option 1: Keep local MongoDB for development
services:
  mongodb:
    image: mongo:7
    container_name: trading-mongodb
    profiles: ['local'] # Only start with 'docker-compose --profile local up'
    # ... rest of config

  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    environment:
      # For local development
      - MONGODB_URI=${MONGODB_URI:-mongodb://admin:admin123@mongodb:27017/?authSource=admin}
      # Override with Atlas for production
      # - MONGODB_URI=${MONGODB_ATLAS_URI}
```

---

#### Task MA-6: MongoDB Atlas Monitoring Setup

**Priority**: 🟢 LOW  
**Estimated Time**: 30 minutes

**Steps**:

1. In Atlas Console → Cluster → Metrics
2. Enable alerts:
   - Connections > 80% of limit
   - Query targeting > 1000 documents
   - Disk space > 80%
3. Set up PagerDuty/Slack integration for alerts

---

### 2.3 MongoDB Atlas Migration Summary

| Task ID | Task Name             | Priority  | Time Est. | Dependencies |
| ------- | --------------------- | --------- | --------- | ------------ |
| MA-1    | Create Atlas Cluster  | 🔴 HIGH   | 45 min    | None         |
| MA-2    | Update Service Config | 🔴 HIGH   | 30 min    | MA-1         |
| MA-3    | Data Migration Script | 🔴 HIGH   | 1 hour    | MA-1, MA-2   |
| MA-4    | Create Indexes        | 🟡 MEDIUM | 30 min    | MA-2         |
| MA-5    | Update Docker Compose | 🟡 MEDIUM | 20 min    | MA-2         |
| MA-6    | Atlas Monitoring      | 🟢 LOW    | 30 min    | MA-1         |

**Total Estimated Time**: ~3.5 hours

---

## 3. Service Integration Points (For Teammate)

> **Note**: These services are handled by another team member. This section provides integration contracts and endpoints they should implement.

### 3.1 News/Crawler Service Integration

**Expected Service**: `article-service` (to be created)  
**Port**: 8084  
**Database**: MongoDB (can share Atlas cluster)

**Integration Contract**:

```yaml
# API Contract for Article Service
endpoints:
  - GET /api/articles
    description: Get paginated list of articles
    query_params:
      - page: int (default: 0)
      - size: int (default: 20)
      - symbol: string (optional, e.g., "BTCUSDT")
      - source: string (optional, e.g., "coindesk")
    response: Page<ArticleDto>

  - GET /api/articles/{id}
    description: Get article by ID
    response: ArticleDto

  - GET /api/articles/latest
    description: Get latest articles (for real-time feed)
    query_params:
      - limit: int (default: 10)
    response: List<ArticleDto>

# RabbitMQ Integration
exchanges:
  - name: article.exchange
    type: topic
    bindings:
      - routing_key: article.new
        queue: article-new-queue
      - routing_key: article.sentiment
        queue: article-sentiment-queue

# Article DTO Structure
ArticleDto:
  id: string
  title: string
  content: string
  source: string
  sourceUrl: string
  publishedAt: datetime
  crawledAt: datetime
  symbols: List<string>  # Related symbols (e.g., ["BTC", "ETH"])
  sentiment:
    score: float  # -1.0 to 1.0
    label: enum [BULLISH, BEARISH, NEUTRAL]
  category: enum [NEWS, ANALYSIS, ANNOUNCEMENT]
```

**API Gateway Route** (to add when service is ready):

```yaml
# api-gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: article-service
          uri: lb://article-service
          predicates:
            - Path=/api/articles/**
          filters:
            - StripPrefix=0
```

---

### 3.2 AI Analysis Service Integration

**Expected Service**: `ai-service` (to be created)  
**Port**: 8085  
**Access**: VIP users only

**Integration Contract**:

```yaml
# API Contract for AI Service
endpoints:
  - POST /api/ai/analyze
    description: Request AI analysis for a symbol
    auth: VIP or ADMIN role required
    request_body:
      symbol: string (required, e.g., "BTCUSDT")
      timeframe: string (optional, e.g., "1h", "4h", "1d")
    response: AnalysisResultDto
    rate_limit: 10 requests/minute for VIP

  - GET /api/ai/predictions/{symbol}
    description: Get cached predictions for a symbol
    auth: VIP or ADMIN role required
    response: List<PredictionDto>

# RabbitMQ Integration (Async Processing)
exchanges:
  - name: ai.exchange
    type: topic
    bindings:
      - routing_key: ai.request
        queue: ai-request-queue
        priority: 3  # Lower priority than price updates
      - routing_key: ai.result
        queue: ai-result-queue

# Response DTOs
AnalysisResultDto:
  requestId: string
  symbol: string
  timestamp: datetime
  prediction:
    direction: enum [UP, DOWN, NEUTRAL]
    confidence: float  # 0.0 to 1.0
    timeframe: string
  reasoning: List<string>  # Explanation points
  relatedNews: List<ArticleSummaryDto>

PredictionDto:
  id: string
  symbol: string
  predictedAt: datetime
  targetTime: datetime
  direction: enum [UP, DOWN]
  confidence: float
  actualDirection: enum [UP, DOWN, PENDING]  # Filled after target time
```

**API Gateway Route** (to add when service is ready):

```yaml
# VIP-only route with role check
- id: ai-service-vip
  uri: lb://ai-service
  predicates:
    - Path=/api/ai/**
    - Header=X-User-Role, VIP|ADMIN
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 10
        redis-rate-limiter.burstCapacity: 20
```

---

### 3.3 Frontend Integration Points

**File**: `Front-end/services/articleService.ts` (EXISTS - may need update)

```typescript
// Expected API calls for News/Article service
export const articleService = {
  getArticles: (params: { page?: number; size?: number; symbol?: string }) =>
    api.get('/api/articles', { params }),

  getLatestArticles: (limit: number = 10) =>
    api.get('/api/articles/latest', { params: { limit } }),

  getArticleById: (id: string) => api.get(`/api/articles/${id}`),
};

// Expected API calls for AI service (VIP only)
export const aiService = {
  requestAnalysis: (symbol: string, timeframe?: string) =>
    api.post('/api/ai/analyze', { symbol, timeframe }),

  getPredictions: (symbol: string) => api.get(`/api/ai/predictions/${symbol}`),
};
```

---

## 4. Production Readiness Tasks

### 4.1 Security Hardening

#### Task PR-1: Enable WebSocket Authentication

**Priority**: 🔴 HIGH  
**Estimated Time**: 2 hours

**Current Issue**: WebSocket endpoints in price-service are unauthenticated.

**File**: `price-service/src/main/java/org/example/priceservice/config/WebSocketSecurityConfig.java` (NEW)

```java
package org.example.priceservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor
                    .getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authToken = accessor.getFirstNativeHeader("Authorization");
                    if (authToken != null && authToken.startsWith("Bearer ")) {
                        String token = authToken.substring(7);
                        Authentication auth = jwtTokenValidator.validateToken(token);
                        accessor.setUser(auth);
                    }
                }
                return message;
            }
        });
    }
}
```

---

#### Task PR-2: API Gateway Rate Limiting

**Priority**: 🔴 HIGH  
**Estimated Time**: 1 hour

**File**: `api-gateway/src/main/resources/application.yml` (UPDATE)

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100
            redis-rate-limiter.burstCapacity: 200
            key-resolver: '#{@userKeyResolver}'

  # Add Redis for rate limiting
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:redis123}
```

---

#### Task PR-3: Health Check Endpoints

**Priority**: 🟡 MEDIUM  
**Estimated Time**: 30 minutes

Ensure all services expose `/actuator/health` with:

- Database connectivity check
- RabbitMQ connectivity check
- Redis connectivity check (where applicable)

---

### 4.2 Observability

#### Task PR-4: Centralized Logging

**Priority**: 🟡 MEDIUM  
**Estimated Time**: 1 hour

Add structured logging with correlation IDs across all services.

**File**: `common/logback-spring.xml` (Template)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>correlationId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <customFields>{"service":"${appName}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

---

## 5. Implementation Priority Matrix

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PRIORITY MATRIX                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│     HIGH IMPACT                                                          │
│     ▲                                                                    │
│     │   ┌─────────────────┐    ┌─────────────────┐                     │
│     │   │ Firebase Email  │    │ WebSocket Auth  │                     │
│     │   │ (FE-1 to FE-4)  │    │ (PR-1)          │                     │
│     │   └─────────────────┘    └─────────────────┘                     │
│     │                                                                    │
│     │   ┌─────────────────┐    ┌─────────────────┐                     │
│     │   │ MongoDB Atlas   │    │ Rate Limiting   │                     │
│     │   │ (MA-1 to MA-3)  │    │ (PR-2)          │                     │
│     │   └─────────────────┘    └─────────────────┘                     │
│     │                                                                    │
│     │   ┌─────────────────┐                                            │
│     │   │ K8s Autoscaling │  ──▶ See kubernetes_autoscaling_plan.md    │
│     │   └─────────────────┘                                            │
│     │                                                                    │
│     │                          ┌─────────────────┐                     │
│     │                          │ Email Templates │                     │
│     │                          │ (FE-5)          │                     │
│     │                          └─────────────────┘                     │
│     │                                                                    │
│     │                          ┌─────────────────┐                     │
│     │                          │ Atlas Monitoring│                     │
│     │                          │ (MA-6)          │                     │
│     │                          └─────────────────┘                     │
│     │                                                                    │
│     └────────────────────────────────────────────────────────▶          │
│                                                            HIGH URGENCY  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Timeline Estimation

### Week 1: Core Infrastructure

| Day | Tasks                             | Estimated Hours |
| --- | --------------------------------- | --------------- |
| 1-2 | FE-1, FE-2, FE-3 (Firebase Setup) | 4               |
| 2-3 | FE-4, FE-6, FE-7 (Email Service)  | 3               |
| 3-4 | MA-1, MA-2 (Atlas Setup)          | 2               |
| 4-5 | MA-3, MA-4 (Migration)            | 2               |

### Week 2: Security & Production Readiness

| Day | Tasks                              | Estimated Hours |
| --- | ---------------------------------- | --------------- |
| 1-2 | PR-1 (WebSocket Auth)              | 4               |
| 2-3 | PR-2 (Rate Limiting)               | 2               |
| 3-4 | FE-5, FE-8 (Templates & Interface) | 2               |
| 4-5 | Testing & Documentation            | 4               |

### Week 3: Kubernetes Setup

See `kubernetes_autoscaling_plan.md` for detailed K8s implementation timeline.

---

## Summary

### Deliverables for Part 5

1. ✅ **Firebase Email Service** - Full implementation with Gmail SMTP
2. ✅ **MongoDB Atlas Migration** - Complete migration plan with scripts
3. ✅ **Integration Contracts** - API specifications for teammate's services
4. ✅ **Production Readiness** - Security and observability improvements

### Next Document

➡️ See `kubernetes_autoscaling_plan.md` for detailed Kubernetes autoscaling implementation for price-service.

---

## References

1. Firebase Admin SDK: https://firebase.google.com/docs/admin/setup
2. Spring Mail: https://docs.spring.io/spring-framework/reference/integration/email.html
3. MongoDB Atlas: https://www.mongodb.com/docs/atlas/
4. Thymeleaf: https://www.thymeleaf.org/documentation.html
