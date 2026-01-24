# Trading Platform Backend - TODO Part 4: Phase Completion & Final Readiness

**Document Version**: 4.0  
**Created**: January 24, 2026  
**Focus**: Microservice Phase Completion, Phase 5 Readiness Assessment, Google/Firebase Integration

---

## Executive Summary

### Current Status Assessment

**✅ COMPLETED (Part 3 Implementation)**:

- ✅ User Subscription System (REGULAR/VIP)
- ✅ Subscription Management Service
- ✅ Password Recovery Flow
- ✅ Email Service (ready for Gmail SMTP)
- ✅ Role-Based Access Control (USER, VIP, ADMIN roles)
- ✅ JWT with roles
- ✅ User Profile endpoints

**❌ MISSING CRITICAL FEATURES**:

- ❌ **Google OAuth 2.0** - NO implementation found (UI buttons exist but backend missing)
- ❌ **User Settings/Preferences** - NO configuration system for user preferences
- ❌ **Admin Dashboard** - Mentioned in architecture but not implemented
- ❌ **Article Service** - Still in monolith, not migrated to microservice
- ❌ **Payment Gateway Integration** - Only mock implementation exists
- ❌ **WebSocket Authentication** - WebSocket endpoints still unauthenticated

### Microservices Migration Status

| Service              | Port | Status         | Completeness | Notes                        |
| -------------------- | ---- | -------------- | ------------ | ---------------------------- |
| **discovery-server** | 8761 | ✅ Running     | 100%         | Eureka service registry      |
| **api-gateway**      | 8081 | ✅ Running     | 90%          | Missing rate limiting config |
| **user-service**     | 8082 | ✅ Running     | 85%          | Missing OAuth, user settings |
| **price-service**    | 8083 | ✅ Running     | 95%          | Missing WebSocket auth       |
| **article-service**  | -    | ❌ Not Created | 0%           | Still in monolith `src/`     |

**Current Phase**: **Phase 3-4 (Transitioning to Phase 5)**  
**Readiness for Phase 5**: **60%** - Critical features missing

---

## Table of Contents

1. [User Settings Analysis](#1-user-settings-analysis)
2. [Google OAuth vs Firebase Decision](#2-google-oauth-vs-firebase-decision)
3. [Missing Features from Core Requirements](#3-missing-features-from-core-requirements)
4. [Microservice Completion Checklist](#4-microservice-completion-checklist)
5. [Phase 5 Blockers](#5-phase-5-blockers)
6. [Recommended Action Plan](#6-recommended-action-plan)
7. [Priority Matrix](#7-priority-matrix)
8. [Timeline Estimation](#8-timeline-estimation)

---

## 1. User Settings Analysis

### Current State: ❌ NO USER SETTINGS SYSTEM

**Search Results**:

- No `UserSettings` entity found
- No `UserPreferences` entity found
- No settings/preferences endpoints
- No configuration storage

### What Should Exist

According to [CoreRequirements.md](core/CoreRequirements.md) and [Features.md](core/Features.md):

**User Settings Should Include**:

1. **Chart Preferences**
   - Default timeframe (1m, 5m, 15m, 1h, 4h, 1d)
   - Default currency pairs
   - Chart theme (light/dark)
   - Technical indicators preferences

2. **Notification Preferences**
   - Email notifications (price alerts, news)
   - Push notifications enabled/disabled
   - Notification frequency

3. **Display Preferences**
   - Language (en, vi, etc.)
   - Timezone
   - Date/time format
   - Currency display format

4. **Privacy Settings**
   - Profile visibility
   - Data sharing preferences

5. **API Settings** (for VIP users)
   - API key management
   - Rate limit display
   - Webhook configurations

### Implementation Required

#### Task BE-SETTINGS-1: Create UserSettings Entity

**File**: `user-service/src/main/java/org/example/userservice/model/UserSettings.java` (NEW)

```java
package org.example.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_settings")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSettings {
    @Id
    private String id;

    private String userId; // Reference to User

    // Chart Preferences
    @Builder.Default
    private String defaultTimeframe = "1h";

    @Builder.Default
    private String defaultSymbol = "BTCUSDT";

    @Builder.Default
    private String chartTheme = "dark";

    private String[] favoriteSymbols; // BTCUSDT, ETHUSDT, etc.

    private String[] enabledIndicators; // MA, RSI, MACD, etc.

    // Notification Preferences
    @Builder.Default
    private Boolean emailNotifications = true;

    @Builder.Default
    private Boolean pushNotifications = false;

    @Builder.Default
    private String notificationFrequency = "realtime"; // realtime, hourly, daily

    // Display Preferences
    @Builder.Default
    private String language = "en";

    @Builder.Default
    private String timezone = "UTC";

    @Builder.Default
    private String dateFormat = "YYYY-MM-DD";

    @Builder.Default
    private String currency = "USD";

    // Privacy Settings
    @Builder.Default
    private Boolean profileVisible = false;

    @Builder.Default
    private Boolean shareAnalytics = true;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Task BE-SETTINGS-2: Create SettingsService

**File**: `user-service/src/main/java/org/example/userservice/service/SettingsService.java` (NEW)

```java
package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.UserSettingsDto;
import org.example.userservice.dto.UpdateSettingsRequest;
import org.example.userservice.model.UserSettings;
import org.example.userservice.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {
    private final UserSettingsRepository settingsRepository;

    public UserSettingsDto getSettings(String userId) {
        UserSettings settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));
        return toDto(settings);
    }

    @Transactional
    public UserSettingsDto updateSettings(String userId, UpdateSettingsRequest request) {
        UserSettings settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));

        // Update fields from request
        if (request.getDefaultTimeframe() != null) {
            settings.setDefaultTimeframe(request.getDefaultTimeframe());
        }
        // ... other fields

        settings.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(settings);

        return toDto(settings);
    }

    private UserSettings createDefaultSettings(String userId) {
        UserSettings settings = UserSettings.builder()
            .userId(userId)
            .createdAt(LocalDateTime.now())
            .build();
        return settingsRepository.save(settings);
    }

    private UserSettingsDto toDto(UserSettings settings) {
        // Conversion logic
        return UserSettingsDto.builder().build();
    }
}
```

#### Task BE-SETTINGS-3: Create Settings Endpoints

**File**: `user-service/src/main/java/org/example/userservice/controller/SettingsController.java` (NEW)

```java
@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "User settings and preferences")
public class SettingsController {
    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserSettingsDto> getSettings() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(settingsService.getSettings(userId));
    }

    @PutMapping
    public ResponseEntity<UserSettingsDto> updateSettings(
            @Valid @RequestBody UpdateSettingsRequest request) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(settingsService.updateSettings(userId, request));
    }

    @PatchMapping("/chart")
    public ResponseEntity<UserSettingsDto> updateChartSettings(
            @RequestBody ChartSettingsRequest request) {
        // Partial update for chart settings only
    }

    @PatchMapping("/notifications")
    public ResponseEntity<UserSettingsDto> updateNotificationSettings(
            @RequestBody NotificationSettingsRequest request) {
        // Partial update for notification settings only
    }
}
```

**Estimated Effort**: 6-8 hours  
**Priority**: HIGH

---

## 2. Google OAuth vs Firebase Decision

### Analysis: Use Google OAuth 2.0 (NOT Firebase)

**Rationale for Google OAuth over Firebase**:

| Criteria                        | Google OAuth 2.0       | Firebase Auth                  | Recommendation |
| ------------------------------- | ---------------------- | ------------------------------ | -------------- |
| **Simplicity**                  | ✅ Simpler             | ❌ More complex                | OAuth wins     |
| **Free Tier**                   | ✅ Unlimited           | ✅ Free up to 10k users        | Both free      |
| **Backend Control**             | ✅ Full control        | ⚠️ Client-side heavy           | OAuth wins     |
| **Learning Curve**              | ✅ Standard OAuth flow | ❌ Firebase SDK learning       | OAuth wins     |
| **Spring Boot Integration**     | ✅ Native support      | ❌ Requires custom integration | OAuth wins     |
| **Production Ready**            | ✅ Already using JWT   | ❌ Would need migration        | OAuth wins     |
| **Existing Code Compatibility** | ✅ Compatible          | ❌ Would break existing auth   | OAuth wins     |

**DECISION**: **Use Google OAuth 2.0 with Spring Security**

### Implementation Plan

#### Task BE-OAUTH-1: Add Google OAuth Dependencies

**File**: `user-service/pom.xml`

```xml
<!-- Add these dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>

<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.19.0</version>
</dependency>
```

#### Task BE-OAUTH-2: Configure Google OAuth

**File**: `user-service/src/main/resources/application.yml`

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - email
              - profile
            redirect-uri: '{baseUrl}/api/auth/oauth2/callback/google'
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
            user-name-attribute: sub

google:
  client:
    id: ${GOOGLE_CLIENT_ID}
    secret: ${GOOGLE_CLIENT_SECRET}
```

#### Task BE-OAUTH-3: Create OAuth Service

**File**: `user-service/src/main/java/org/example/userservice/service/OAuth2Service.java` (NEW)

```java
package org.example.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.TokenResponse;
import org.example.userservice.model.Role;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${google.client.id}")
    private String googleClientId;

    @Transactional
    public TokenResponse authenticateWithGoogle(String idTokenString) {
        try {
            // Verify Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
            )
            .setAudience(Collections.singletonList(googleClientId))
            .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            if (!emailVerified) {
                throw new IllegalArgumentException("Email not verified by Google");
            }

            // Find or create user
            User user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, name, pictureUrl));

            if (!user.isActive()) {
                throw new IllegalStateException("Account is deactivated");
            }

            // Update last login
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(user.getId(), user.getRoles());
            String refreshToken = jwtService.generateRefreshToken(user.getId());

            log.info("User {} authenticated via Google OAuth", email);

            return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();

        } catch (Exception e) {
            log.error("Google OAuth authentication failed", e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    private User createGoogleUser(String email, String name, String pictureUrl) {
        User user = User.builder()
            .email(email)
            .fullName(name)
            .password(null) // No password for OAuth users
            .oauthProvider("google")
            .emailVerified(true) // Google already verified
            .subscriptionType(SubscriptionType.REGULAR)
            .subscriptionStartDate(LocalDateTime.now())
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        user.syncRolesWithSubscription();

        log.info("Created new user via Google OAuth: {}", email);
        return userRepository.save(user);
    }
}
```

#### Task BE-OAUTH-4: Create OAuth Endpoints

**File**: `user-service/src/main/java/org/example/userservice/controller/OAuth2Controller.java` (NEW)

```java
package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.GoogleTokenRequest;
import org.example.userservice.dto.TokenResponse;
import org.example.userservice.service.OAuth2Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2", description = "OAuth 2.0 authentication APIs")
public class OAuth2Controller {
    private final OAuth2Service oauth2Service;

    @PostMapping("/google")
    @Operation(summary = "Authenticate with Google OAuth 2.0",
            description = "Exchange Google ID token for JWT access/refresh tokens")
    public ResponseEntity<TokenResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleTokenRequest request) {
        log.info("Google OAuth authentication attempt");
        TokenResponse tokens = oauth2Service.authenticateWithGoogle(request.getIdToken());
        return ResponseEntity.ok(tokens);
    }
}
```

#### Task BE-OAUTH-5: Update .env

```bash
# Google OAuth 2.0 Credentials
GOOGLE_CLIENT_ID=your-app-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-secret-key

# Get these from: https://console.cloud.google.com/apis/credentials
```

**Setup Instructions**:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URIs:
   - `http://localhost:3000` (frontend)
   - `http://localhost:8081/api/auth/oauth2/callback/google` (backend)
6. Copy Client ID and Client Secret to `.env`

**Estimated Effort**: 8-10 hours  
**Priority**: CRITICAL

---

## 3. Missing Features from Core Requirements

### Features Analysis from [CoreRequirements.md](core/CoreRequirements.md)

| Requirement                      | Status          | Service                | Priority   |
| -------------------------------- | --------------- | ---------------------- | ---------- |
| **1. News Collection (Crawler)** | ⚠️ In Monolith  | article-service needed | HIGH       |
| **2. Price Chart Display**       | ✅ Implemented  | price-service          | DONE       |
| **3. AI/NLP Analysis**           | ❌ Not Started  | nlp-service needed     | EXCLUDED\* |
| **4. Account Management**        | ⚠️ 85% Complete | user-service           | HIGH       |

_\*As per user: "the article dynamic-html crawler and the AI-predictive, article analysis is being handled by someone else"_

### Account Management Gaps

From requirement #4 in [CoreRequirements.md](core/CoreRequirements.md):

**Expected Features**:

- ✅ User registration
- ✅ User login
- ✅ Subscription tiers (REGULAR/VIP)
- ❌ **User preferences** - MISSING
- ❌ **Google OAuth** - MISSING
- ✅ Password recovery
- ⚠️ Profile management (partial - no update endpoint fully tested)

---

## 4. Microservice Completion Checklist

### Discovery Server (discovery-server) ✅ 100%

- [x] Eureka server running
- [x] Port 8761 accessible
- [x] Services registering correctly
- [x] Dockerfile created
- [x] docker-compose integration

**Status**: COMPLETE

---

### API Gateway (api-gateway) ⚠️ 90%

**Completed**:

- [x] Gateway routing
- [x] CORS configuration
- [x] JWT validation filter
- [x] Service discovery integration
- [x] Dockerfile

**Missing**:

- [ ] Rate limiting configuration
- [ ] Circuit breaker (Resilience4j)
- [ ] Request/response logging
- [ ] API versioning strategy

**Tasks**:

#### Task BE-GATEWAY-1: Add Rate Limiting

```yaml
# api-gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/auth/**,/api/user/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: '#{@userKeyResolver}'
```

#### Task BE-GATEWAY-2: Add Circuit Breaker

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

**Estimated Effort**: 4-6 hours  
**Priority**: MEDIUM

---

### User Service (user-service) ⚠️ 85%

**Completed**:

- [x] Registration/Login/Logout
- [x] JWT generation and refresh
- [x] Subscription system (REGULAR/VIP)
- [x] Password recovery
- [x] Email service
- [x] Role-based access control
- [x] MongoDB integration
- [x] Dockerfile

**Missing**:

- [ ] Google OAuth 2.0 (CRITICAL)
- [ ] User settings/preferences (HIGH)
- [ ] Profile update endpoint (tested)
- [ ] Account deactivation endpoint
- [ ] User search/admin endpoints

**Tasks**: See sections 1 and 2 above

**Estimated Remaining Effort**: 14-18 hours  
**Priority**: CRITICAL

---

### Price Service (price-service) ⚠️ 95%

**Completed**:

- [x] Historical price data API
- [x] WebSocket real-time prices
- [x] Candle aggregation (1m, 5m, 15m, 1h, 4h, 1d)
- [x] Multiple symbol support
- [x] PostgreSQL integration
- [x] Redis caching
- [x] Binance API client
- [x] Dockerfile

**Missing**:

- [ ] WebSocket authentication (users can connect without auth)
- [ ] TimescaleDB migration (Phase 5)
- [ ] Continuous aggregates (Phase 5)

**Tasks**:

#### Task BE-PRICE-1: Add WebSocket Authentication

**File**: `price-service/src/main/java/org/example/priceservice/config/WebSocketSecurityConfig.java`

```java
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
            .simpDestMatchers("/topic/**").authenticated()
            .simpSubscribeDestMatchers("/topic/**").authenticated()
            .anyMessage().authenticated();
        return messages.build();
    }
}
```

**File**: `price-service/src/main/java/org/example/priceservice/filter/WebSocketJwtFilter.java`

```java
@Component
public class WebSocketJwtFilter implements ChannelInterceptor {
    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            if (authToken != null && authToken.startsWith("Bearer ")) {
                String token = authToken.substring(7);
                // Validate JWT and set authentication
                String userId = jwtService.validateToken(token);
                accessor.setUser(() -> userId);
            }
        }
        return message;
    }
}
```

**Estimated Effort**: 4-6 hours  
**Priority**: HIGH

---

### Article Service (article-service) ❌ 0% - NOT CREATED

**Current State**: Article-related code still in monolith `src/main/java/com/example/backend/`

**Files in Monolith**:

- `src/main/java/com/example/backend/controller/ArticleController.java`
- `src/main/java/com/example/backend/service/ArticleService.java`
- `src/main/java/com/example/backend/service/CrawlerService.java`
- `src/main/java/com/example/backend/model/Article.java`
- `src/main/java/com/example/backend/repository/ArticleRepository.java`

**Required Work**:

1. Create new `article-service` module
2. Migrate article-related code
3. Set up MongoDB connection
4. Create Dockerfile
5. Add to docker-compose.yml
6. Update API Gateway routing

**RECOMMENDATION**:

Since the user stated _"the article dynamic-html crawler and the AI-predictive, article analysis is being handled by someone else"_, we should:

**Option A**: Keep article code in monolith for now (teammate will handle migration)  
**Option B**: Create basic article-service skeleton for future migration

**Decision Needed**: Clarify with teammate's timeline

**Estimated Effort (if we do it)**: 12-16 hours  
**Priority**: DEFERRED (waiting for teammate)

---

## 5. Phase 5 Blockers

### Critical Blockers

| Blocker                          | Impact                                          | Service         | Effort   |
| -------------------------------- | ----------------------------------------------- | --------------- | -------- |
| **No Google OAuth**              | Users can't sign in with Google (UI expects it) | user-service    | 8-10h    |
| **No User Settings**             | Users can't customize experience                | user-service    | 6-8h     |
| **WebSocket Unauth**             | Security vulnerability                          | price-service   | 4-6h     |
| **Article Service Not Migrated** | Monolith still in use                           | article-service | DEFERRED |

### High Priority Items

| Item                            | Service       | Effort |
| ------------------------------- | ------------- | ------ |
| Rate limiting on API Gateway    | api-gateway   | 4h     |
| Profile update endpoint testing | user-service  | 2h     |
| TimescaleDB migration           | price-service | 6-8h   |
| OpenAPI documentation           | all services  | 6-8h   |

---

## 6. Recommended Action Plan

### Phase A: Complete Microservice Phase (Before Phase 5)

**Week 1 (January 25-29, 2026)**:

#### Day 1-2: User Service Completion

- [ ] Implement Google OAuth 2.0 (8-10h)
  - Set up Google Cloud Console
  - Add OAuth dependencies
  - Create OAuth2Service
  - Create OAuth endpoints
  - Test with frontend

#### Day 3-4: User Settings System

- [ ] Implement User Settings (6-8h)
  - Create UserSettings entity
  - Create SettingsService
  - Create Settings endpoints
  - Frontend integration

#### Day 5: Security & Testing

- [ ] WebSocket Authentication (4-6h)
  - Add WebSocket JWT filter
  - Test authenticated connections
- [ ] API Gateway Rate Limiting (4h)
- [ ] Integration testing (4h)

**Total Week 1**: 26-36 hours

---

### Phase B: Phase 5 Preparation (Week 2)

**Week 2 (January 30 - February 3, 2026)**:

#### Day 1: Documentation

- [ ] OpenAPI/Swagger documentation for all services (6-8h)
- [ ] Update README files
- [ ] Create deployment documentation

#### Day 2-3: Performance & Optimization

- [ ] TimescaleDB migration (price-service) (6-8h)
- [ ] Redis caching optimization
- [ ] Load testing

#### Day 4: Docker & Kubernetes

- [ ] Optimize Dockerfiles (multi-stage builds)
- [ ] Create Kubernetes manifests
- [ ] Helm charts (optional)

#### Day 5: Final Testing

- [ ] End-to-end testing
- [ ] Performance benchmarking
- [ ] Security audit

**Total Week 2**: 20-24 hours

---

## 7. Priority Matrix

### Critical (Must Have for Phase 5)

| Priority | Task           | Service       | Effort | Rationale                                |
| -------- | -------------- | ------------- | ------ | ---------------------------------------- |
| 🔴 P0    | Google OAuth   | user-service  | 8-10h  | Frontend expects it, users can't sign in |
| 🔴 P0    | User Settings  | user-service  | 6-8h   | Core user experience requirement         |
| 🔴 P0    | WebSocket Auth | price-service | 4-6h   | Security vulnerability                   |

**Total P0**: 18-24 hours

### High (Should Have)

| Priority | Task          | Service       | Effort |
| -------- | ------------- | ------------- | ------ |
| 🟡 P1    | Rate Limiting | api-gateway   | 4h     |
| 🟡 P1    | TimescaleDB   | price-service | 6-8h   |
| 🟡 P1    | OpenAPI Docs  | all           | 6-8h   |

**Total P1**: 16-20 hours

### Medium (Nice to Have)

| Priority | Task                 | Service      | Effort |
| -------- | -------------------- | ------------ | ------ |
| 🟢 P2    | Circuit Breaker      | api-gateway  | 3-4h   |
| 🟢 P2    | Account Deactivation | user-service | 2-3h   |
| 🟢 P2    | Admin Endpoints      | user-service | 8-10h  |

**Total P2**: 13-17 hours

### Low (Future)

| Priority | Task                        | Service         | Effort   |
| -------- | --------------------------- | --------------- | -------- |
| ⚪ P3    | Article Service Migration   | article-service | DEFERRED |
| ⚪ P3    | Payment Gateway Integration | user-service    | DEFERRED |
| ⚪ P3    | Admin Dashboard             | admin-service   | EXCLUDED |

---

## 8. Timeline Estimation

### Conservative Timeline (Recommended)

```
┌─────────────────────────────────────────────────────────────────┐
│ Week 1: Complete Microservice Phase (Jan 25-29)                 │
├─────────────────────────────────────────────────────────────────┤
│ Mon-Tue:  Google OAuth (8-10h)                                  │
│ Wed-Thu:  User Settings (6-8h)                                  │
│ Fri:      WebSocket Auth + Rate Limiting (8-10h)                │
├─────────────────────────────────────────────────────────────────┤
│ Total: 22-28 hours                                              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Week 2: Phase 5 Readiness (Jan 30 - Feb 3)                      │
├─────────────────────────────────────────────────────────────────┤
│ Mon:      OpenAPI Documentation (6-8h)                          │
│ Tue-Wed:  TimescaleDB Migration (6-8h)                          │
│ Thu:      Docker/Kubernetes Prep (6-8h)                         │
│ Fri:      Final Testing (6-8h)                                  │
├─────────────────────────────────────────────────────────────────┤
│ Total: 24-32 hours                                              │
└─────────────────────────────────────────────────────────────────┘

GRAND TOTAL: 46-60 hours (~ 2 weeks full-time or 3 weeks part-time)
```

### Aggressive Timeline (If Deadline is Tight)

```
┌─────────────────────────────────────────────────────────────────┐
│ Days 1-3: Critical Features Only (Jan 25-27)                    │
├─────────────────────────────────────────────────────────────────┤
│ Day 1:    Google OAuth (10h)                                    │
│ Day 2:    User Settings (8h)                                    │
│ Day 3:    WebSocket Auth + Rate Limiting (8h)                   │
├─────────────────────────────────────────────────────────────────┤
│ Total: 26 hours                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Days 4-5: Phase 5 Minimal (Jan 28-29)                           │
├─────────────────────────────────────────────────────────────────┤
│ Day 4:    OpenAPI + Documentation (8h)                          │
│ Day 5:    Final Testing (8h)                                    │
├─────────────────────────────────────────────────────────────────┤
│ Total: 16 hours                                                 │
└─────────────────────────────────────────────────────────────────┘

GRAND TOTAL: 42 hours (~ 1 week full-time)
Note: Defers TimescaleDB, Kubernetes prep to later
```

---

## 9. Success Criteria

### Microservice Phase Complete When:

- [x] ✅ All 4 core services running (discovery, gateway, user, price)
- [ ] ❌ Google OAuth working end-to-end
- [ ] ❌ User settings CRUD working
- [ ] ❌ WebSocket requires authentication
- [x] ✅ Subscription system working (REGULAR/VIP)
- [x] ✅ Password recovery working
- [x] ✅ JWT with roles working
- [ ] ⚠️ API Gateway has rate limiting
- [ ] ⚠️ All services have OpenAPI docs

**Current Progress**: **7/9 (78%)** → **Need 2-3 more items for 100%**

### Phase 5 Ready When:

- [ ] All microservice phase items above complete
- [ ] TimescaleDB migration done
- [ ] Docker images optimized (multi-stage builds)
- [ ] Kubernetes manifests created
- [ ] Load testing passed (>100 concurrent users)
- [ ] Security audit passed
- [ ] Deployment documentation complete
- [ ] Frontend integration tested

**Current Progress**: **0/8 (0%)** → **Not ready for Phase 5 yet**

---

## 10. Risks & Mitigation

| Risk                                | Impact   | Probability | Mitigation                          |
| ----------------------------------- | -------- | ----------- | ----------------------------------- |
| **Google OAuth integration bugs**   | HIGH     | MEDIUM      | Allocate 2 extra days for debugging |
| **WebSocket auth breaks frontend**  | HIGH     | LOW         | Test incrementally, keep fallback   |
| **TimescaleDB migration data loss** | CRITICAL | LOW         | Backup database before migration    |
| **Rate limiting too strict**        | MEDIUM   | MEDIUM      | Make configurable via .env          |
| **Article service teammate delay**  | LOW      | MEDIUM      | Keep monolith article code working  |

---

## 11. Next Steps (Immediate Actions)

### Today (January 24, 2026)

1. **Review this document** with team
2. **Confirm priorities** and timeline
3. **Set up Google Cloud Console** for OAuth credentials
4. **Create feature branches**:
   - `feature/google-oauth`
   - `feature/user-settings`
   - `feature/websocket-auth`

### Tomorrow (January 25, 2026)

1. **Start Google OAuth implementation**
2. **Update .env with Google credentials**
3. **Create OAuth2Service and endpoints**

### This Week

- Complete all P0 (Critical) tasks
- Begin P1 (High) tasks
- Daily standups to track progress

---

## 12. Appendix: Environment Variables Needed

### Add to `.env`

```bash
# ====================================
# Google OAuth 2.0
# ====================================
GOOGLE_CLIENT_ID=your-app-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-secret-key

# Get from: https://console.cloud.google.com/apis/credentials
# Authorized redirect URIs:
# - http://localhost:3000
# - http://localhost:8081/api/auth/oauth2/callback/google

# ====================================
# Email Service (Gmail SMTP)
# ====================================
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-16-char-app-password
SMTP_FROM=Trading Platform <noreply@tradingplatform.com>

# Get app password from: https://myaccount.google.com/apppasswords

# ====================================
# Frontend Configuration
# ====================================
FRONTEND_URL=http://localhost:3000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# ====================================
# Rate Limiting (Redis)
# ====================================
RATE_LIMIT_LOGIN=10  # requests per minute
RATE_LIMIT_API=100   # requests per minute
RATE_LIMIT_WS=50     # connections per minute

# ====================================
# Feature Flags
# ====================================
FEATURE_GOOGLE_OAUTH=true
FEATURE_USER_SETTINGS=true
FEATURE_WEBSOCKET_AUTH=true
FEATURE_TIMESCALEDB=false  # Enable in Phase 5
```

---

## 13. References

- [CoreRequirements.md](core/CoreRequirements.md) - Business requirements
- [Features.md](core/Features.md) - Feature specifications
- [updated_todo_part3.md](updated_todo_part3.md) - Previous TODO (Part 3)
- [Phase5-ImplementationGuide.md](guides/Phase5-ImplementationGuide.md) - Next phase guide
- [MIGRATION_STATUS.md](../MIGRATION_STATUS.md) - Microservices migration status

---

## 14. Summary

### Current State

**Microservices**: 4/5 services running (article-service deferred)  
**Completeness**: 60-70% ready for Phase 5  
**Critical Gaps**: Google OAuth, User Settings, WebSocket Auth

### Recommended Path Forward

1. **Week 1**: Complete critical features (OAuth, Settings, WebSocket Auth)
2. **Week 2**: Phase 5 preparation (TimescaleDB, Kubernetes, Testing)
3. **Week 3**: Final testing and deployment

### Decision Points

- ✅ **Use Google OAuth 2.0** (NOT Firebase) - Simpler, free, Spring Boot native
- ⏸️ **Defer Article Service** - Wait for teammate (crawler/NLP handler)
- ✅ **Implement User Settings** - Core user experience requirement
- ✅ **Add WebSocket Auth** - Security vulnerability fix

### Final Recommendation

**Focus on P0 (Critical) tasks first**:

1. Google OAuth (8-10h)
2. User Settings (6-8h)
3. WebSocket Auth (4-6h)

**Total**: 18-24 hours → **Can be done in 3-4 days of focused work**

Once these are complete, the microservice phase is **90%+ done** and ready to move to Phase 5.

---

**Document End**

_Last Updated: January 24, 2026_  
_Next Review: January 27, 2026 (after Week 1 tasks)_
