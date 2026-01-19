# Trading Platform Backend - Updated TODO Part 3

**Document Version**: 3.0  
**Created**: January 19, 2026  
**Focus**: User Management, Subscription System (REGULAR/VIP), OAuth Integration, Password Recovery

---

## Executive Summary

This document addresses **critical missing features** identified through comprehensive code analysis:

### Key Findings:
- ✅ **Basic auth works**: Register, Login, Logout, Token Refresh
- ❌ **NO user types**: No REGULAR/VIP subscription system
- ❌ **NO OAuth**: Google sign-in not implemented (UI buttons exist but non-functional)
- ❌ **NO password recovery**: Forgot password flow missing entirely
- ❌ **NO profile updates**: Users cannot update information (endpoint commented out)
- ❌ **NO role-based access**: No RBAC or permissions system
- ⚠️ **Security gaps**: WebSocket unauthenticated, JWT filter lacks proper validation

**Scope Notes**:
- ✅ Web crawler and NLP service **excluded** (handled by teammate)
- ✅ Admin Dashboard **excluded** (not needed for current scope)
- ✅ Database migration **not needed** (fresh implementation)
- ✅ Payment gateway **mock only** (low priority)

---

## Table of Contents

1. [🔴 CRITICAL: User Subscription System (REGULAR/VIP)](#1-critical-user-subscription-system-regularvip)
2. [🔴 CRITICAL: OAuth 2.0 Google Sign-In](#2-critical-oauth-20-google-sign-in)
3. [🔴 CRITICAL: Password Recovery Flow](#3-critical-password-recovery-flow)
4. [🟡 HIGH: User Profile Management](#4-high-user-profile-management)
5. [🟡 HIGH: Role-Based Access Control](#5-high-role-based-access-control)
6. [🔴 CRITICAL: Security Enhancements](#6-critical-security-enhancements)
7. [🟡 HIGH: Email Service (Gmail SMTP)](#7-high-email-service-gmail-smtp)
8. [🟢 LOW: Mock Payment Gateway](#8-low-mock-payment-gateway)
9. [Testing Requirements](#9-testing-requirements)

---

## 1. 🔴 CRITICAL: User Subscription System (REGULAR/VIP)

**Priority**: CRITICAL | **Effort**: 4-5 hours | **Assignee**: Backend Team

### Current State Analysis:

**File**: `user-service/src/main/java/org/example/userservice/model/User.java`

```java
@Document(collection = "users")
public class User {
    @Id private String id;
    private String email;
    private String password;
    private String fullName;
    // ❌ NO subscription type
    // ❌ NO subscription expiry
    // ❌ NO account tier
}
```

### Implementation Tasks:

#### ✅ Task BE-SUB-1: Create Subscription Enum

**File**: `user-service/src/main/java/org/example/userservice/model/SubscriptionType.java` (NEW)

```java
package org.example.userservice.model;

public enum SubscriptionType {
    REGULAR("Regular", 0.0, "Basic trading features + Real-time prices"),
    VIP("VIP", 29.99, "All REGULAR features + AI Analysis + Priority Support + Advanced Charts");

    private final String displayName;
    private final Double monthlyPrice;
    private final String description;

    SubscriptionType(String displayName, Double monthlyPrice, String description) {
        this.displayName = displayName;
        this.monthlyPrice = monthlyPrice;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public Double getMonthlyPrice() { return monthlyPrice; }
    public String getDescription() { return description; }
}
```

**Why only REGULAR/VIP?**
- Simpler user model
- Clear value proposition
- Easier to test and maintain
- VIP price point ($29.99) captures serious traders

---

#### ✅ Task BE-SUB-2: Update User Entity

**File**: `user-service/src/main/java/org/example/userservice/model/User.java`

**Changes to make:**

```java
package org.example.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    private String id;
    
    private String email;
    private String password; // BCrypt hashed (null for OAuth users)
    private String fullName;
    
    // ========== NEW SUBSCRIPTION FIELDS ==========
    @Builder.Default
    private SubscriptionType subscriptionType = SubscriptionType.REGULAR;
    
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate; // null = lifetime/REGULAR
    
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Builder.Default
    private Boolean isActive = true;
    
    // ========== AUDIT FIELDS ==========
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // ========== PROFILE FIELDS ==========
    private String profilePictureUrl;
    private String phoneNumber;
    
    // ========== BUSINESS LOGIC ==========
    public boolean isVip() {
        if (subscriptionType != SubscriptionType.VIP) return false;
        if (subscriptionEndDate == null) return true; // Lifetime VIP
        return LocalDateTime.now().isBefore(subscriptionEndDate);
    }
    
    public int getDaysUntilExpiry() {
        if (subscriptionEndDate == null) return -1; // Never expires
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(), 
            subscriptionEndDate
        );
        return (int) Math.max(0, days);
    }
}
```

**Key Design Decisions:**
- `subscriptionEndDate = null` means **lifetime** or **REGULAR** (no expiry)
- VIP subscription requires `subscriptionEndDate` to be set
- `isActive = false` for soft-deleted accounts

---

#### ✅ Task BE-SUB-3: Create Subscription DTOs

**File**: `user-service/src/main/java/org/example/userservice/dto/SubscriptionDto.java` (NEW)

```java
package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.model.SubscriptionType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionDto {
    private SubscriptionType type;
    private String displayName;
    private Double monthlyPrice;
    private String description;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate; // null for REGULAR or lifetime VIP
    
    private Boolean isActive;
    private Integer daysRemaining; // -1 for lifetime, 0+ for countdown
}
```

**File**: `user-service/src/main/java/org/example/userservice/dto/UpgradeSubscriptionRequest.java` (NEW)

```java
package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.model.SubscriptionType;

import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpgradeSubscriptionRequest {
    @NotNull(message = "Target subscription type is required")
    private SubscriptionType targetType;
    
    @NotNull(message = "Duration in months is required (1-12)")
    private Integer durationMonths; // 1, 3, 6, 12
    
    private String promoCode; // Optional for future use
}
```

---

#### ✅ Task BE-SUB-4: Create SubscriptionService

**File**: `user-service/src/main/java/org/example/userservice/service/SubscriptionService.java` (NEW)

```java
package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.SubscriptionDto;
import org.example.userservice.dto.UpgradeSubscriptionRequest;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final UserRepository userRepository;
    
    public SubscriptionDto getCurrentSubscription(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return SubscriptionDto.builder()
            .type(user.getSubscriptionType())
            .displayName(user.getSubscriptionType().getDisplayName())
            .monthlyPrice(user.getSubscriptionType().getMonthlyPrice())
            .description(user.getSubscriptionType().getDescription())
            .startDate(user.getSubscriptionStartDate())
            .endDate(user.getSubscriptionEndDate())
            .isActive(user.isVip() || user.getSubscriptionType() == SubscriptionType.REGULAR)
            .daysRemaining(user.getDaysUntilExpiry())
            .build();
    }
    
    @Transactional
    public SubscriptionDto upgradeToVip(String userId, UpgradeSubscriptionRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Validate request
        if (request.getTargetType() != SubscriptionType.VIP) {
            throw new IllegalArgumentException("Can only upgrade to VIP");
        }
        
        if (request.getDurationMonths() < 1 || request.getDurationMonths() > 12) {
            throw new IllegalArgumentException("Duration must be 1-12 months");
        }
        
        // Check if already VIP
        if (user.isVip()) {
            throw new IllegalArgumentException("Already a VIP member. Use renewal instead.");
        }
        
        // Mock payment processing (see Section 8 for details)
        boolean paymentSuccess = mockProcessPayment(user, request);
        
        if (!paymentSuccess) {
            throw new RuntimeException("Payment processing failed");
        }
        
        // Upgrade subscription
        user.setSubscriptionType(SubscriptionType.VIP);
        user.setSubscriptionStartDate(LocalDateTime.now());
        user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(request.getDurationMonths()));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        log.info("User {} upgraded to VIP for {} months", userId, request.getDurationMonths());
        
        // TODO: Send confirmation email (see Section 7)
        
        return getCurrentSubscription(userId);
    }
    
    @Transactional
    public SubscriptionDto renewVipSubscription(String userId, Integer additionalMonths) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        if (user.getSubscriptionType() != SubscriptionType.VIP) {
            throw new IllegalArgumentException("Only VIP subscriptions can be renewed");
        }
        
        // Extend from current end date (or now if expired)
        LocalDateTime baseDate = user.getSubscriptionEndDate();
        if (baseDate == null || baseDate.isBefore(LocalDateTime.now())) {
            baseDate = LocalDateTime.now();
        }
        
        user.setSubscriptionEndDate(baseDate.plusMonths(additionalMonths));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User {} renewed VIP for {} months", userId, additionalMonths);
        
        return getCurrentSubscription(userId);
    }
    
    @Transactional
    public void cancelVipSubscription(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        if (user.getSubscriptionType() != SubscriptionType.VIP) {
            throw new IllegalArgumentException("Not a VIP subscriber");
        }
        
        // Downgrade to REGULAR
        user.setSubscriptionType(SubscriptionType.REGULAR);
        user.setSubscriptionEndDate(null); // REGULAR has no expiry
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User {} cancelled VIP subscription", userId);
        
        // TODO: Send cancellation confirmation email
    }
    
    public List<SubscriptionType> getAvailablePlans() {
        return Arrays.asList(SubscriptionType.values());
    }
    
    private boolean mockProcessPayment(User user, UpgradeSubscriptionRequest request) {
        // MOCK IMPLEMENTATION - See Section 8 for architecture
        double totalAmount = SubscriptionType.VIP.getMonthlyPrice() * request.getDurationMonths();
        log.info("MOCK PAYMENT: User {} charged ${} for {} months VIP", 
            user.getEmail(), totalAmount, request.getDurationMonths());
        
        // Always succeed in mock
        return true;
    }
}
```

---

#### ✅ Task BE-SUB-5: Create Subscription Controller

**File**: `user-service/src/main/java/org/example/userservice/controller/SubscriptionController.java` (NEW)

```java
package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.SubscriptionDto;
import org.example.userservice.dto.UpgradeSubscriptionRequest;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscription", description = "User subscription management APIs")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;
    
    @GetMapping("/me")
    @Operation(summary = "Get current user subscription details")
    public ResponseEntity<SubscriptionDto> getMySubscription() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(userId));
    }
    
    @GetMapping("/plans")
    @Operation(summary = "Get available subscription plans")
    public ResponseEntity<List<SubscriptionType>> getAvailablePlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }
    
    @PostMapping("/upgrade")
    @Operation(summary = "Upgrade to VIP subscription")
    public ResponseEntity<SubscriptionDto> upgradeToVip(
        @RequestBody @Valid UpgradeSubscriptionRequest request
    ) {
        String userId = getCurrentUserId();
        SubscriptionDto result = subscriptionService.upgradeToVip(userId, request);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/renew")
    @Operation(summary = "Renew VIP subscription")
    public ResponseEntity<SubscriptionDto> renewVipSubscription(
        @RequestParam Integer months
    ) {
        String userId = getCurrentUserId();
        SubscriptionDto result = subscriptionService.renewVipSubscription(userId, months);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/cancel")
    @Operation(summary = "Cancel VIP subscription (downgrade to REGULAR)")
    public ResponseEntity<?> cancelVipSubscription() {
        String userId = getCurrentUserId();
        subscriptionService.cancelVipSubscription(userId);
        return ResponseEntity.ok(Map.of(
            "message", "VIP subscription cancelled. You now have REGULAR access."
        ));
    }
    
    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();
    }
}
```

---

#### ✅ Task BE-SUB-6: Update AuthService Registration

**File**: `user-service/src/main/java/org/example/userservice/service/AuthService.java`

**Modify the `registerUser` method:**

```java
public UserDto registerUser(String email, String password, String fullName) {
    // Check duplicate email
    Optional<User> userOption = userMongoRepository.findByEmail(email);
    if (userOption.isPresent()) {
        throw new UserAlreadyExistsException("Email already exists");
    }

    // Create new user with REGULAR subscription
    User newUser = User.builder()
        .email(email)
        .fullName(fullName)
        .password(passwordEncoder.encode(password))
        .subscriptionType(SubscriptionType.REGULAR)  // ✅ Default tier
        .emailVerified(false)  // ✅ Require verification
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();
        
    userMongoRepository.save(newUser);
    
    log.info("New user registered: {} (REGULAR tier)", email);
    
    // TODO: Send welcome + verification email (Section 7)
    
    return new UserDto(
        newUser.getId(), 
        newUser.getFullName(), 
        newUser.getEmail(), 
        newUser.getSubscriptionType()
    );
}
```

---

#### ✅ Task BE-SUB-7: Update UserDto

**File**: `user-service/src/main/java/org/example/userservice/dto/UserDto.java`

```java
package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.model.SubscriptionType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String id;
    private String fullName;
    private String email;
    
    // ✅ NEW FIELDS
    private SubscriptionType subscriptionType;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private String profilePictureUrl;
    
    // Constructor for backward compatibility
    public UserDto(String id, String fullName, String email, SubscriptionType subscriptionType) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.subscriptionType = subscriptionType;
    }
}
```

---

#### ✅ Task BE-SUB-8: Add Routes to API Gateway

**File**: `api-gateway/src/main/resources/application.yml`

**Add after existing routes:**

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            # ... existing user-service and price-service routes ...
            
            # Subscription Management
            - id: subscription-service
              uri: lb://user-service
              predicates:
                - Path=/api/subscription/**
            
            - id: subscription-service-doc
              uri: lb://user-service
              predicates:
                - Path=/v3/api-docs/subscription-service
              filters:
                - RewritePath=/v3/api-docs/subscription-service, /v3/api-docs
```

---

### 📋 Checklist for Section 1:

- [ ] BE-SUB-1: Create `SubscriptionType` enum with REGULAR and VIP
- [ ] BE-SUB-2: Update User entity with subscription fields
- [ ] BE-SUB-3: Create SubscriptionDto and UpgradeSubscriptionRequest
- [ ] BE-SUB-4: Implement SubscriptionService with upgrade/renew/cancel
- [ ] BE-SUB-5: Create SubscriptionController with REST endpoints
- [ ] BE-SUB-6: Update registration to default to REGULAR tier
- [ ] BE-SUB-7: Update UserDto to include subscription info
- [ ] BE-SUB-8: Add subscription routes to API Gateway
- [ ] BE-SUB-9: Test upgrade flow: REGULAR → VIP
- [ ] BE-SUB-10: Test downgrade flow: VIP → REGULAR
- [ ] BE-SUB-11: Update Swagger/OpenAPI documentation

**Estimated Time**: 4-5 hours

---

## 2. 🔴 CRITICAL: OAuth 2.0 Google Sign-In

**Priority**: CRITICAL | **Effort**: 6-8 hours | **Assignee**: Backend Team

### Current State:
- ❌ NO OAuth configuration in SecurityConfig
- ❌ NO Spring OAuth2 dependencies
- ❌ Frontend has Google button but backend endpoint missing

### Why Google OAuth?
- Most users have Google accounts
- Better UX (no password to remember)
- Google handles security (MFA, breach detection)
- Email is auto-verified

---

### Implementation Tasks:

#### ✅ Task BE-OAUTH-1: Add OAuth2 Dependencies

**File**: `user-service/pom.xml`

**Add these dependencies:**

```xml
<dependencies>
    <!-- Existing dependencies... -->
    
    <!-- OAuth 2.0 Client Support -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
    
    <!-- Google API Client for token verification -->
    <dependency>
        <groupId>com.google.api-client</groupId>
        <artifactId>google-api-client</artifactId>
        <version>2.2.0</version>
    </dependency>
</dependencies>
```

---

#### ✅ Task BE-OAUTH-2: Configure Google OAuth Credentials

**File**: `user-service/src/main/resources/application.yml`

**Add OAuth configuration:**

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
            redirect-uri: "{baseUrl}/login/oauth2/code/google"
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
            user-name-attribute: sub
```

**File**: `micro.env.example`

**Add these environment variables:**

```bash
# Google OAuth 2.0 Configuration
# Get credentials from: https://console.cloud.google.com/apis/credentials
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret

# Authorized redirect URIs (configure in Google Console):
# - http://localhost:8082/login/oauth2/code/google (development)
# - https://yourdomain.com/login/oauth2/code/google (production)
```

**Setup Steps:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project "Trading Platform"
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URIs
6. Copy Client ID and Client Secret to `micro.env`

---

#### ✅ Task BE-OAUTH-3: Create OAuth DTOs

**File**: `user-service/src/main/java/org/example/userservice/dto/OAuth2UserInfo.java` (NEW)

```java
package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OAuth2UserInfo {
    private String id;           // Google's unique user ID (sub)
    private String email;
    private String name;
    private String picture;
    private Boolean emailVerified;
    
    /**
     * Parse Google OAuth2 attributes into UserInfo
     * Google payload structure:
     * {
     *   "sub": "1234567890",
     *   "email": "user@gmail.com",
     *   "email_verified": true,
     *   "name": "John Doe",
     *   "picture": "https://lh3.googleusercontent.com/..."
     * }
     */
    public static OAuth2UserInfo fromGoogleAttributes(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
            .id((String) attributes.get("sub"))
            .email((String) attributes.get("email"))
            .name((String) attributes.get("name"))
            .picture((String) attributes.get("picture"))
            .emailVerified((Boolean) attributes.get("email_verified"))
            .build();
    }
}
```

**File**: `user-service/src/main/java/org/example/userservice/dto/GoogleTokenRequest.java` (NEW)

```java
package org.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleTokenRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken; // JWT from Google Sign-In
}
```

---

#### ✅ Task BE-OAUTH-4: Create OAuth Service

**File**: `user-service/src/main/java/org/example/userservice/service/OAuth2Service.java` (NEW)

```java
package org.example.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.OAuth2UserInfo;
import org.example.userservice.dto.TokenResponse;
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
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    
    /**
     * Verify Google ID token and process OAuth login/registration
     */
    @Transactional
    public TokenResponse processGoogleLogin(String idTokenString) throws Exception {
        // Verify Google ID token
        OAuth2UserInfo userInfo = verifyGoogleIdToken(idTokenString);
        
        // Find or create user
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        
        User user;
        if (existingUser.isPresent()) {
            // Existing user - update OAuth info
            user = existingUser.get();
            user.setFullName(userInfo.getName());
            user.setProfilePictureUrl(userInfo.getPicture());
            user.setEmailVerified(userInfo.getEmailVerified());
            user.setUpdatedAt(LocalDateTime.now());
            
            log.info("OAuth2 login for existing user: {}", user.getEmail());
        } else {
            // New user - create account via OAuth
            user = User.builder()
                .email(userInfo.getEmail())
                .fullName(userInfo.getName())
                .password(null) // ✅ No password for OAuth users
                .profilePictureUrl(userInfo.getPicture())
                .emailVerified(userInfo.getEmailVerified()) // ✅ Google verified
                .subscriptionType(SubscriptionType.REGULAR) // ✅ Default tier
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
            
            log.info("Created new user via OAuth2: {}", user.getEmail());
        }
        
        userRepository.save(user);
        
        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        
        return new TokenResponse(accessToken, refreshToken);
    }
    
    /**
     * Verify Google ID token using Google's official library
     * This prevents token forgery attacks
     */
    private OAuth2UserInfo verifyGoogleIdToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
            new NetHttpTransport(), 
            new GsonFactory()
        )
        .setAudience(Collections.singletonList(googleClientId))
        .build();
        
        GoogleIdToken idToken = verifier.verify(idTokenString);
        
        if (idToken == null) {
            throw new SecurityException("Invalid Google ID token");
        }
        
        GoogleIdToken.Payload payload = idToken.getPayload();
        
        return OAuth2UserInfo.builder()
            .id(payload.getSubject())
            .email(payload.getEmail())
            .name((String) payload.get("name"))
            .picture((String) payload.get("picture"))
            .emailVerified(payload.getEmailVerified())
            .build();
    }
}
```

**Security Note**: This implementation uses Google's official verification library to prevent token forgery. Never decode and trust tokens without verification!

---

#### ✅ Task BE-OAUTH-5: Create OAuth Controller

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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2", description = "Social login APIs")
public class OAuth2Controller {
    private final OAuth2Service oauth2Service;
    
    /**
     * Google OAuth callback endpoint
     * Frontend sends Google ID token for verification
     * 
     * Flow:
     * 1. User clicks "Sign in with Google" on frontend
     * 2. Google Sign-In button generates ID token
     * 3. Frontend sends ID token to this endpoint
     * 4. Backend verifies token with Google
     * 5. Backend creates/updates user
     * 6. Backend returns JWT access + refresh tokens
     */
    @PostMapping("/google")
    @Operation(summary = "Sign in with Google")
    public ResponseEntity<TokenResponse> googleLogin(
        @RequestBody @Valid GoogleTokenRequest request
    ) {
        try {
            TokenResponse tokenResponse = oauth2Service.processGoogleLogin(request.getIdToken());
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("Google OAuth error: {}", e.getMessage());
            throw new BadCredentialsException("Invalid Google token: " + e.getMessage());
        }
    }
}
```

---

#### ✅ Task BE-OAUTH-6: Update Security Config

**File**: `user-service/src/main/java/org/example/userservice/config/SecurityConfig.java`

**Add OAuth endpoint to public routes:**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
            .requestMatchers("/api/auth/oauth2/**").permitAll()  // ✅ Allow OAuth endpoints
            .requestMatchers("/api/health/**").permitAll()
            .requestMatchers("/v3/**", "/swagger-ui/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

---

#### ✅ Task BE-OAUTH-7: Add Route to API Gateway

**File**: `api-gateway/src/main/resources/application.yml`

```yaml
# Add after existing auth routes
- id: oauth2-service
  uri: lb://user-service
  predicates:
    - Path=/api/auth/oauth2/**
```

---

### 📋 Checklist for Section 2:

- [ ] BE-OAUTH-1: Add Spring OAuth2 and Google API dependencies
- [ ] BE-OAUTH-2: Configure Google OAuth in application.yml
- [ ] BE-OAUTH-3: Create OAuth2UserInfo and GoogleTokenRequest DTOs
- [ ] BE-OAUTH-4: Implement OAuth2Service with Google token verification
- [ ] BE-OAUTH-5: Create OAuth2Controller
- [ ] BE-OAUTH-6: Update SecurityConfig to allow OAuth endpoints
- [ ] BE-OAUTH-7: Add OAuth routes to API Gateway
- [ ] BE-OAUTH-8: Get Google OAuth credentials from Google Cloud Console
- [ ] BE-OAUTH-9: Configure authorized redirect URIs in Google Console
- [ ] BE-OAUTH-10: Test OAuth flow end-to-end with frontend
- [ ] BE-OAUTH-11: Update API documentation with OAuth endpoints

**Estimated Time**: 6-8 hours

**Google Cloud Console Setup Required**: ~30 minutes

---

## 3. 🔴 CRITICAL: Password Recovery Flow

**Priority**: CRITICAL | **Effort**: 5-6 hours | **Assignee**: Backend Team

### Current State:
- ❌ NO `/api/auth/password/forgot` endpoint
- ❌ NO `PasswordResetToken` entity
- ❌ NO email service for reset links
- ❌ Frontend has "Forgot Password?" link but goes to `#`

### Flow Overview:

```
User clicks "Forgot Password"
    ↓
Frontend: Enter email → POST /api/auth/password/forgot
    ↓
Backend: Generate reset token → Save to MongoDB → Send email
    ↓
User: Clicks link in email → Frontend shows reset form
    ↓
Frontend: POST /api/auth/password/reset {token, newPassword}
    ↓
Backend: Validate token → Update password → Revoke token
    ↓
User: Can login with new password
```

---

### Implementation Tasks:

#### ✅ Task BE-PWD-1: Create Password Reset Token Entity

**File**: `user-service/src/main/java/org/example/userservice/model/PasswordResetToken.java` (NEW)

```java
package org.example.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "password_reset_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetToken {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String token; // UUID
    
    private String userId;
    private String email; // For convenience
    
    @Indexed(expireAfterSeconds = 3600) // Auto-delete after 1 hour
    private Date expiresAt;
    
    @Builder.Default
    private Boolean used = false;
    
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return new Date().after(expiresAt);
    }
}
```

**MongoDB Index**: The `@Indexed(expireAfterSeconds = 3600)` creates a TTL index that automatically deletes expired tokens after 1 hour.

---

#### ✅ Task BE-PWD-2: Create Repository

**File**: `user-service/src/main/java/org/example/userservice/repository/PasswordResetTokenRepository.java` (NEW)

```java
package org.example.userservice.repository;

import org.example.userservice.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    @Query("{ 'email': ?0, 'used': false, 'expiresAt': { $gt: ?1 } }")
    List<PasswordResetToken> findValidTokensByEmail(String email, Date now);
    
    void deleteByUserId(String userId);
}
```

---

#### ✅ Task BE-PWD-3: Create Password Reset DTOs

**File**: `user-service/src/main/java/org/example/userservice/dto/ForgotPasswordRequest.java` (NEW)

```java
package org.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
```

**File**: `user-service/src/main/java/org/example/userservice/dto/ResetPasswordRequest.java` (NEW)

```java
package org.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
```

---

#### ✅ Task BE-PWD-4: Create Password Service

**File**: `user-service/src/main/java/org/example/userservice/service/PasswordService.java` (NEW)

```java
package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exception.InvalidTokenException;
import org.example.userservice.model.PasswordResetToken;
import org.example.userservice.model.User;
import org.example.userservice.repository.PasswordResetTokenRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // Will create in Section 7
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    /**
     * Initiate password reset flow
     * Security: Always return success, even if email doesn't exist (prevent user enumeration)
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists - security best practice
            log.warn("Password reset requested for non-existent email: {}", email);
            return; // Silently succeed
        }
        
        User user = userOpt.get();
        
        // Check if user signed up via OAuth (no password)
        if (user.getPassword() == null) {
            log.warn("Password reset requested for OAuth user: {}", email);
            // TODO: Send email explaining they use OAuth
            return;
        }
        
        // Invalidate all existing tokens for this user
        resetTokenRepository.deleteByUserId(user.getId());
        
        // Generate reset token
        String token = UUID.randomUUID().toString();
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .userId(user.getId())
            .email(user.getEmail())
            .expiresAt(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .used(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        resetTokenRepository.save(resetToken);
        
        // Build reset link
        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
        
        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
        
        log.info("Password reset email sent to: {}", email);
    }
    
    /**
     * Reset password with token
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Find token
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));
        
        // Validate token
        if (resetToken.getUsed()) {
            throw new InvalidTokenException("Reset token already used");
        }
        
        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Reset token has expired");
        }
        
        // Find user and update password
        User user = userRepository.findById(resetToken.getUserId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
        
        log.info("Password reset successful for user: {}", user.getEmail());
        
        // TODO: Send confirmation email
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFullName());
        
        // TODO: Invalidate all refresh tokens for security
    }
    
    /**
     * Validate reset token without consuming it
     * Used by frontend to check if token is valid before showing reset form
     */
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        return !resetToken.getUsed() && !resetToken.isExpired();
    }
}
```

---

#### ✅ Task BE-PWD-5: Create InvalidTokenException

**File**: `user-service/src/main/java/org/example/userservice/exception/InvalidTokenException.java` (NEW)

```java
package org.example.userservice.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
```

**File**: `user-service/src/main/java/org/example/userservice/exception/GlobalExceptionHandler.java`

**Add handler for InvalidTokenException:**

```java
@ExceptionHandler(InvalidTokenException.class)
public ResponseEntity<ErrorResponseDto> handleInvalidToken(InvalidTokenException ex) {
    ErrorResponseDto error = new ErrorResponseDto(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
}
```

---

#### ✅ Task BE-PWD-6: Create Password Controller

**File**: `user-service/src/main/java/org/example/userservice/controller/PasswordController.java` (NEW)

```java
package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.ForgotPasswordRequest;
import org.example.userservice.dto.ResetPasswordRequest;
import org.example.userservice.service.PasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Password", description = "Password recovery APIs")
public class PasswordController {
    private final PasswordService passwordService;
    
    @PostMapping("/forgot")
    @Operation(summary = "Request password reset link")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        passwordService.initiatePasswordReset(request.getEmail());
        
        // Always return success (security - don't reveal if email exists)
        return ResponseEntity.ok(Map.of(
            "message", "If that email exists, a password reset link has been sent"
        ));
    }
    
    @PostMapping("/reset")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordService.resetPassword(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(Map.of(
            "message", "Password reset successful. You can now log in with your new password."
        ));
    }
    
    @GetMapping("/reset/validate")
    @Operation(summary = "Validate reset token (check if token is valid)")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordService.validateResetToken(token);
        
        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", "Invalid or expired token"
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "valid", true
        ));
    }
}
```

---

#### ✅ Task BE-PWD-7: Update Security Config

**File**: `user-service/src/main/java/org/example/userservice/config/SecurityConfig.java`

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
    .requestMatchers("/api/auth/oauth2/**").permitAll()
    .requestMatchers("/api/auth/password/**").permitAll()  // ✅ Allow password reset
    .requestMatchers("/api/health/**").permitAll()
    // ... rest
)
```

---

### 📋 Checklist for Section 3:

- [ ] BE-PWD-1: Create PasswordResetToken entity with TTL index
- [ ] BE-PWD-2: Create PasswordResetTokenRepository
- [ ] BE-PWD-3: Create ForgotPasswordRequest and ResetPasswordRequest DTOs
- [ ] BE-PWD-4: Implement PasswordService
- [ ] BE-PWD-5: Create InvalidTokenException
- [ ] BE-PWD-6: Create PasswordController
- [ ] BE-PWD-7: Update SecurityConfig to allow password endpoints
- [ ] BE-PWD-8: Configure MongoDB TTL index for auto-deletion
- [ ] BE-PWD-9: Test forgot password flow
- [ ] BE-PWD-10: Test reset password with valid token
- [ ] BE-PWD-11: Test reset password with expired/used token
- [ ] BE-PWD-12: Add rate limiting to prevent abuse (5 requests/hour per email)

**Estimated Time**: 5-6 hours

---

## 4. 🟡 HIGH: User Profile Management

**Priority**: HIGH | **Effort**: 3-4 hours | **Assignee**: Backend Team

### Current State:
- ✅ `GET /api/user/me` exists and works
- ❌ `PUT /api/user/me` endpoint is **COMMENTED OUT**
- ❌ NO change password endpoint
- ❌ NO account deletion

### Implementation Tasks:

#### ✅ Task BE-PROFILE-1: Enable Profile Update Endpoint

**File**: `user-service/src/main/java/org/example/userservice/controller/UserController.java`

**Uncomment and enhance:**

```java
package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.*;
import org.example.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "User", description = "User profile management APIs")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok().body(userService.getUserById());
    }
    
    // ✅ UNCOMMENT AND ENHANCE
    @PutMapping("/me")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        String userId = getCurrentUserId();
        UserDto updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(updated);
    }
    
    @PatchMapping("/me/password")
    @Operation(summary = "Change password")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        String userId = getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    @DeleteMapping("/me")
    @Operation(summary = "Delete account (soft delete)")
    public ResponseEntity<?> deleteAccount(@RequestBody @Valid DeleteAccountRequest request) {
        String userId = getCurrentUserId();
        userService.deleteAccount(userId, request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
    
    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();
    }
}
```

---

#### ✅ Task BE-PROFILE-2: Create Profile DTOs

**File**: `user-service/src/main/java/org/example/userservice/dto/UpdateProfileRequest.java` (NEW)

```java
package org.example.userservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    private String profilePictureUrl; // URL to uploaded image
}
```

**File**: `user-service/src/main/java/org/example/userservice/dto/ChangePasswordRequest.java` (NEW)

```java
package org.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
```

**File**: `user-service/src/main/java/org/example/userservice/dto/DeleteAccountRequest.java` (NEW)

```java
package org.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteAccountRequest {
    @NotBlank(message = "Password confirmation required to delete account")
    private String password;
}
```

---

#### ✅ Task BE-PROFILE-3: Update UserService

**File**: `user-service/src/main/java/org/example/userservice/service/UserService.java`

**Add these methods:**

```java
@Transactional
public UserDto updateProfile(String userId, UpdateProfileRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    // Update only provided fields (null values = no update)
    if (request.getFullName() != null && !request.getFullName().isBlank()) {
        user.setFullName(request.getFullName());
    }
    
    if (request.getPhoneNumber() != null) {
        user.setPhoneNumber(request.getPhoneNumber());
    }
    
    if (request.getProfilePictureUrl() != null) {
        user.setProfilePictureUrl(request.getProfilePictureUrl());
    }
    
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
    
    log.info("User profile updated: {}", userId);
    
    return mapToDto(user);
}

@Transactional
public void changePassword(String userId, ChangePasswordRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    // Check if OAuth user (no password)
    if (user.getPassword() == null) {
        throw new IllegalStateException("Cannot change password for OAuth users");
    }
    
    // Verify current password
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
        throw new BadCredentialsException("Current password is incorrect");
    }
    
    // Check if new password is same as old
    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
        throw new IllegalArgumentException("New password must be different from current password");
    }
    
    // Update password
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
    
    log.info("Password changed for user: {}", userId);
    
    // TODO: Invalidate all refresh tokens for security
    // TODO: Send email notification
}

@Transactional
public void deleteAccount(String userId, String password) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    // Verify password (unless OAuth user)
    if (user.getPassword() != null) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Password is incorrect");
        }
    }
    
    // Soft delete - mark as inactive
    user.setIsActive(false);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
    
    log.info("Account soft-deleted: {}", userId);
    
    // TODO: Invalidate all tokens
    // TODO: Schedule hard deletion after 30 days
    // TODO: Send confirmation email
}

private UserDto mapToDto(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setEmail(user.getEmail());
    dto.setFullName(user.getFullName());
    dto.setSubscriptionType(user.getSubscriptionType());
    dto.setEmailVerified(user.getEmailVerified());
    dto.setProfilePictureUrl(user.getProfilePictureUrl());
    dto.setCreatedAt(user.getCreatedAt());
    return dto;
}
```

---

### 📋 Checklist for Section 4:

- [ ] BE-PROFILE-1: Uncomment and enhance profile update endpoint
- [ ] BE-PROFILE-2: Create UpdateProfileRequest, ChangePasswordRequest, DeleteAccountRequest
- [ ] BE-PROFILE-3: Implement updateProfile(), changePassword(), deleteAccount() in UserService
- [ ] BE-PROFILE-4: Test profile update with valid data
- [ ] BE-PROFILE-5: Test change password with correct/incorrect current password
- [ ] BE-PROFILE-6: Test account deletion with password verification
- [ ] BE-PROFILE-7: Handle OAuth users (no password) gracefully

**Estimated Time**: 3-4 hours

---

## 5. 🟡 HIGH: Role-Based Access Control

**Priority**: HIGH | **Effort**: 5-6 hours | **Assignee**: Backend Team

### Why RBAC?

- VIP users should have access to premium features
- Future: Moderators can manage content
- Endpoint-level security via `@PreAuthorize("hasRole('VIP')")`
- Currently: JWT has empty authorities list

### Implementation Tasks:

#### ✅ Task BE-RBAC-1: Create Role Enum

**File**: `user-service/src/main/java/org/example/userservice/model/Role.java` (NEW)

```java
package org.example.userservice.model;

public enum Role {
    USER("ROLE_USER", "Regular user"),
    VIP("ROLE_VIP", "VIP subscriber");
    
    private final String authority;
    private final String description;
    
    Role(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }
    
    public String getAuthority() {
        return authority;
    }
    
    public String getDescription() {
        return description;
    }
}
```

**Note**: Keeping it simple - USER and VIP roles map to subscription types.

---

#### ✅ Task BE-RBAC-2: Update User Entity

**File**: `user-service/src/main/java/org/example/userservice/model/User.java`

**Add roles field:**

```java
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import java.util.stream.Collectors;

@Document(collection = "users")
@Builder
public class User {
    // ... existing fields ...
    
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    /**
     * Get Spring Security authorities from roles
     */
    public List<GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
            .collect(Collectors.toList());
    }
    
    /**
     * Sync roles with subscription type
     * Called after subscription changes
     */
    public void syncRolesWithSubscription() {
        this.roles.clear();
        this.roles.add(Role.USER); // Everyone has USER role
        
        if (this.isVip()) {
            this.roles.add(Role.VIP); // VIP subscribers get VIP role
        }
    }
}
```

---

#### ✅ Task BE-RBAC-3: Update JwtService to Include Roles

**File**: `user-service/src/main/java/org/example/userservice/service/JwtService.java`

**Modify token generation:**

```java
public String generateAccessToken(String userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    // Sync roles before generating token
    user.syncRolesWithSubscription();
    userRepository.save(user);
    
    Map<String, Object> claims = new HashMap<>();
    claims.put("user_id", userId);
    claims.put("roles", user.getRoles().stream()
        .map(Role::getAuthority)
        .collect(Collectors.toList()));  // ✅ Include roles in JWT
    
    return createToken(claims, userId, ACCESS_TOKEN_EXPIRATION);
}

/**
 * Extract roles from JWT
 */
public List<String> extractRoles(String token) {
    try {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    } catch (Exception e) {
        return Collections.emptyList();
    }
}
```

---

#### ✅ Task BE-RBAC-4: Update JWT Auth Filter

**File**: `user-service/src/main/java/org/example/userservice/filter/JwtAuthFilter.java`

**Extract roles and set authorities:**

```java
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.stream.Collectors;

@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    
    if (header == null || !header.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }
    
    String token = header.replace("Bearer ", "");
    
    // ✅ Validate token expiration and signature
    if (!jwtService.isTokenValid(token)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
        return;
    }
    
    String userId = jwtService.extractUserId(token);
    List<String> roles = jwtService.extractRoles(token);  // ✅ Extract roles
    
    // ✅ Create authorities from roles
    List<GrantedAuthority> authorities = roles.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
    
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(userId, null, authorities);
    
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    filterChain.doFilter(request, response);
}
```

---

#### ✅ Task BE-RBAC-5: Enable Method Security

**File**: `user-service/src/main/java/org/example/userservice/config/SecurityConfig.java`

```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // ✅ Enable @PreAuthorize
public class SecurityConfig {
    // ... existing config ...
}
```

---

#### ✅ Task BE-RBAC-6: Example VIP-Only Endpoint

**File**: `price-service/src/main/java/org/example/priceservice/controller/PriceController.java`

**Add VIP-only advanced analytics endpoint:**

```java
import org.springframework.security.access.prepost.PreAuthorize;

@GetMapping("/analytics/advanced")
@PreAuthorize("hasRole('VIP')")  // ✅ Only VIP users can access
@Operation(summary = "Advanced price analytics (VIP only)")
public ResponseEntity<?> getAdvancedAnalytics(@RequestParam String symbol) {
    // Advanced analytics logic
    return ResponseEntity.ok(Map.of(
        "message", "Advanced analytics for " + symbol,
        "feature", "VIP_ONLY"
    ));
}
```

**Note**: Any endpoint with `@PreAuthorize("hasRole('VIP')")` will return 403 Forbidden for non-VIP users.

---

#### ✅ Task BE-RBAC-7: Update Registration to Assign Roles

**File**: `user-service/src/main/java/org/example/userservice/service/AuthService.java`

```java
public UserDto registerUser(String email, String password, String fullName) {
    // ... existing code ...
    
    User newUser = User.builder()
        .email(email)
        .fullName(fullName)
        .password(passwordEncoder.encode(password))
        .subscriptionType(SubscriptionType.REGULAR)
        .roles(new HashSet<>(Collections.singleton(Role.USER)))  // ✅ Default role
        .emailVerified(false)
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();
    
    userMongoRepository.save(newUser);
    // ... rest
}
```

---

#### ✅ Task BE-RBAC-8: Update Subscription Service to Sync Roles

**File**: `user-service/src/main/java/org/example/userservice/service/SubscriptionService.java`

**Modify `upgradeToVip()` and `cancelVipSubscription()`:**

```java
@Transactional
public SubscriptionDto upgradeToVip(String userId, UpgradeSubscriptionRequest request) {
    // ... existing code ...
    
    // Upgrade subscription
    user.setSubscriptionType(SubscriptionType.VIP);
    user.setSubscriptionStartDate(LocalDateTime.now());
    user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(request.getDurationMonths()));
    user.syncRolesWithSubscription();  // ✅ Sync roles
    user.setUpdatedAt(LocalDateTime.now());
    
    userRepository.save(user);
    // ... rest
}

@Transactional
public void cancelVipSubscription(String userId) {
    // ... existing code ...
    
    user.setSubscriptionType(SubscriptionType.REGULAR);
    user.setSubscriptionEndDate(null);
    user.syncRolesWithSubscription();  // ✅ Sync roles
    user.setUpdatedAt(LocalDateTime.now());
    
    userRepository.save(user);
    // ... rest
}
```

---

### 📋 Checklist for Section 5:

- [ ] BE-RBAC-1: Create Role enum (USER, VIP)
- [ ] BE-RBAC-2: Add roles field to User entity with syncRolesWithSubscription()
- [ ] BE-RBAC-3: Include roles in JWT claims
- [ ] BE-RBAC-4: Update JWT filter to extract and set authorities
- [ ] BE-RBAC-5: Enable @EnableMethodSecurity
- [ ] BE-RBAC-6: Add example VIP-only endpoint
- [ ] BE-RBAC-7: Assign USER role on registration
- [ ] BE-RBAC-8: Sync roles when subscription changes
- [ ] BE-RBAC-9: Test VIP-only endpoints return 403 for REGULAR users
- [ ] BE-RBAC-10: Test VIP-only endpoints work for VIP users

**Estimated Time**: 5-6 hours

---

## 6. 🔴 CRITICAL: Security Enhancements

**Priority**: CRITICAL | **Effort**: 4-5 hours | **Assignee**: Backend Team

### Current Security Issues:

1. ⚠️ **JWT Filter doesn't validate token**: Accepts any JWT without checking expiration/signature
2. ⚠️ **WebSocket completely public**: `/ws/**` has no authentication
3. ⚠️ **No rate limiting**: Login endpoint vulnerable to brute force
4. ⚠️ **No account lockout**: Unlimited login attempts

---

### Implementation Tasks:

#### ✅ Task BE-SEC-1: Fix JWT Filter Validation

**File**: `user-service/src/main/java/org/example/userservice/service/JwtService.java`

**Add token validation method:**

```java
import io.jsonwebtoken.JwtException;

/**
 * Validate JWT token (expiration + signature)
 */
public boolean isTokenValid(String token) {
    try {
        Claims claims = extractAllClaims(token);
        Date expiration = claims.getExpiration();
        return expiration.after(new Date());  // Check if not expired
    } catch (JwtException | IllegalArgumentException e) {
        log.error("Invalid JWT token: {}", e.getMessage());
        return false;
    }
}
```

**Already implemented in BE-RBAC-4** - JWT filter now validates before processing.

---

#### ✅ Task BE-SEC-2: Secure WebSocket Endpoints

**File**: `Back-end/src/main/java/com/example/backend/config/WebSocketConfig.java`

**Add JWT authentication to WebSocket handshake:**

```java
package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    // ... existing broker config ...
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/prices")
            .setAllowedOrigins("http://localhost:3000")
            .addInterceptors(new JwtHandshakeInterceptor())  // ✅ Add JWT validation
            .withSockJS();
    }
}

/**
 * WebSocket handshake interceptor for JWT validation
 * Validates JWT from query params or headers before allowing connection
 */
class JwtHandshakeInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                  ServerHttpResponse response,
                                  WebSocketHandler wsHandler, 
                                  Map<String, Object> attributes) throws Exception {
        // Extract token from query params: /ws/prices?token=xxx
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.split("token=")[1].split("&")[0];
            
            // TODO: Validate JWT token here (inject JwtService)
            // For now, allow all connections
            // In production: validate token, extract userId, set as attribute
            attributes.put("userId", "validated_user_id");
            return true;
        }
        
        // Reject connection if no token
        return false;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, 
                              ServerHttpResponse response,
                              WebSocketHandler wsHandler, 
                              Exception exception) {
        // No-op
    }
}
```

**TODO**: Inject JwtService into interceptor for proper validation.

---

#### ✅ Task BE-SEC-3: Add Rate Limiting to API Gateway

**File**: `api-gateway/pom.xml`

**Add Redis rate limiter dependency:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

**File**: `api-gateway/src/main/resources/application.yml`

**Add rate limit configuration:**

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
  
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: auth-login-rate-limited
              uri: lb://user-service
              predicates:
                - Path=/api/auth/login
              filters:
                - name: RequestRateLimiter
                  args:
                    redis-rate-limiter.replenishRate: 5  # 5 requests per second
                    redis-rate-limiter.burstCapacity: 10  # Max 10 requests in burst
                    redis-rate-limiter.requestedTokens: 1
```

**Explanation**:
- `replenishRate`: 5 requests per second allowed
- `burstCapacity`: Can burst up to 10 requests
- Returns 429 Too Many Requests if exceeded

---

#### ✅ Task BE-SEC-4: Add Account Lockout After Failed Logins

**File**: `user-service/src/main/java/org/example/userservice/model/User.java`

**Add failed login tracking:**

```java
@Document(collection = "users")
public class User {
    // ... existing fields ...
    
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    private LocalDateTime lockedUntil; // null = not locked
    
    public boolean isAccountLocked() {
        if (lockedUntil == null) return false;
        return LocalDateTime.now().isBefore(lockedUntil);
    }
    
    public void incrementFailedLogins() {
        this.failedLoginAttempts++;
        
        // Lock account after 5 failed attempts for 15 minutes
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }
    
    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}
```

**File**: `user-service/src/main/java/org/example/userservice/service/AuthService.java`

**Update login to check lockout:**

```java
public TokenResponse login(String email, String password) {
    Optional<User> authUserExists = userMongoRepository.findByEmail(email);
    if (authUserExists.isEmpty()) {
        throw new BadCredentialsException("Invalid email or password");
    }
    
    User authUser = authUserExists.get();
    
    // ✅ Check if account is locked
    if (authUser.isAccountLocked()) {
        throw new AccountLockedException(
            "Account locked due to too many failed login attempts. Try again in 15 minutes."
        );
    }
    
    // Verify password
    if (!passwordEncoder.matches(password, authUser.getPassword())) {
        // ✅ Increment failed attempts
        authUser.incrementFailedLogins();
        userMongoRepository.save(authUser);
        throw new BadCredentialsException("Invalid email or password");
    }
    
    // ✅ Reset failed attempts on successful login
    if (authUser.getFailedLoginAttempts() > 0) {
        authUser.resetFailedLogins();
        userMongoRepository.save(authUser);
    }
    
    // Generate tokens
    // ... rest of method
}
```

**File**: `user-service/src/main/java/org/example/userservice/exception/AccountLockedException.java` (NEW)

```java
package org.example.userservice.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
```

---

### 📋 Checklist for Section 6:

- [ ] BE-SEC-1: Add isTokenValid() to JwtService ✅ (done in RBAC section)
- [ ] BE-SEC-2: Add JWT validation to WebSocket handshake
- [ ] BE-SEC-3: Configure rate limiting in API Gateway
- [ ] BE-SEC-4: Add account lockout after 5 failed logins
- [ ] BE-SEC-5: Test rate limiting (exceed 5 login attempts/sec)
- [ ] BE-SEC-6: Test account lockout (fail login 5 times)
- [ ] BE-SEC-7: Add account unlock endpoint for admins (future)

**Estimated Time**: 4-5 hours

---

## 7. 🟡 HIGH: Email Service (Gmail SMTP)

**Priority**: HIGH | **Effort**: 3-4 hours | **Assignee**: Backend Team

### Why Gmail SMTP?

✅ **Free** (500 emails/day for personal accounts)  
✅ **Simple setup** (just app password)  
✅ **Works with Next.js, Java, Python** (SMTP universal)  
✅ **Reliable** (Google infrastructure)  
✅ **No credit card** needed

**Limitations**:
- 500 emails/day (sufficient for development)
- Requires "App Password" (not regular Gmail password)

**Alternatives** (for production):
- SendGrid: 100 emails/day free, then paid
- AWS SES: $0.10 per 1000 emails
- Mailgun: 5000 emails/month free

---

### Implementation Tasks:

#### ✅ Task BE-EMAIL-1: Add Email Dependencies

**File**: `user-service/pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Mail -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    
    <!-- Thymeleaf for HTML email templates -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

---

#### ✅ Task BE-EMAIL-2: Configure Gmail SMTP

**File**: `user-service/src/main/resources/application.yml`

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME}  # your-email@gmail.com
    password: ${SMTP_PASSWORD}  # App Password (not regular password)
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

app:
  frontend:
    url: ${FRONTEND_URL:http://localhost:3000}
  email:
    from: ${SMTP_USERNAME}
    from-name: Trading Platform
```

**File**: `micro.env.example`

**Add SMTP configuration:**

```bash
# Email Service (Gmail SMTP)
# Setup: https://support.google.com/accounts/answer/185833
# 1. Enable 2-Step Verification in Google Account
# 2. Generate App Password: https://myaccount.google.com/apppasswords
# 3. Use App Password below (NOT your regular Gmail password)

SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-16-char-app-password

# Frontend URL for email links
FRONTEND_URL=http://localhost:3000
```

**Setup Instructions** (include in README):

1. Go to Google Account Settings
2. Security → 2-Step Verification → Enable
3. Security → App Passwords → Generate
4. Select "Mail" and "Other (custom name)" → "Trading Platform"
5. Copy 16-character password to `micro.env`

---

#### ✅ Task BE-EMAIL-3: Create Email Service

**File**: `user-service/src/main/java/org/example/userservice/service/EmailService.java` (NEW)

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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import org.example.userservice.model.SubscriptionType;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.from-name}")
    private String fromName;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("resetLink", resetLink);
            context.setVariable("expiryHours", 1);
            
            String html = templateEngine.process("emails/password-reset", context);
            
            sendHtmlEmail(
                to, 
                "Reset Your Password - Trading Platform", 
                html
            );
            
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            // Don't fail the operation if email fails
        }
    }
    
    /**
     * Send welcome email on registration
     */
    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("loginUrl", frontendUrl + "/login");
            
            String html = templateEngine.process("emails/welcome", context);
            
            sendHtmlEmail(
                to,
                "Welcome to Trading Platform!",
                html
            );
            
            log.info("Welcome email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }
    
    /**
     * Send password changed confirmation
     */
    @Async
    public void sendPasswordChangedEmail(String to, String name) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("supportEmail", fromEmail);
            
            String html = templateEngine.process("emails/password-changed", context);
            
            sendHtmlEmail(
                to,
                "Password Changed - Trading Platform",
                html
            );
            
            log.info("Password changed email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password changed email to: {}", to, e);
        }
    }
    
    /**
     * Send subscription upgrade confirmation
     */
    @Async
    public void sendSubscriptionUpgradeEmail(String to, String name, SubscriptionType newPlan, int months) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("plan", newPlan.getDisplayName());
            context.setVariable("months", months);
            context.setVariable("price", newPlan.getMonthlyPrice() * months);
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
            
            String html = templateEngine.process("emails/subscription-upgrade", context);
            
            sendHtmlEmail(
                to,
                "Subscription Upgraded - Trading Platform",
                html
            );
            
            log.info("Subscription upgrade email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send subscription upgrade email to: {}", to, e);
        }
    }
    
    /**
     * Internal method to send HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(fromEmail, fromName);
        
        mailSender.send(message);
    }
}
```

**Note**: `@Async` makes email sending non-blocking (doesn't slow down API responses).

---

#### ✅ Task BE-EMAIL-4: Create Email Templates

**File**: `user-service/src/main/resources/templates/emails/password-reset.html` (NEW)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset Your Password</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .button { 
            display: inline-block; 
            padding: 12px 24px; 
            background-color: #3b82f6; 
            color: white; 
            text-decoration: none; 
            border-radius: 5px; 
        }
        .footer { margin-top: 30px; font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <h2>Hello <span th:text="${name}">User</span>,</h2>
        
        <p>You recently requested to reset your password for your Trading Platform account.</p>
        
        <p>Click the button below to reset it:</p>
        
        <p>
            <a th:href="${resetLink}" class="button">Reset Password</a>
        </p>
        
        <p>Or copy this link into your browser:</p>
        <p><a th:href="${resetLink}" th:text="${resetLink}">Reset Link</a></p>
        
        <p>This link will expire in <strong th:text="${expiryHours}">1</strong> hour.</p>
        
        <p>If you didn't request this password reset, please ignore this email or contact support if you have concerns.</p>
        
        <div class="footer">
            <p>© 2026 Trading Platform. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
```

**File**: `user-service/src/main/resources/templates/emails/welcome.html` (NEW)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Welcome to Trading Platform</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .button { 
            display: inline-block; 
            padding: 12px 24px; 
            background-color: #10b981; 
            color: white; 
            text-decoration: none; 
            border-radius: 5px; 
        }
    </style>
</head>
<body>
    <div class="container">
        <h2>Welcome to Trading Platform, <span th:text="${name}">User</span>!</h2>
        
        <p>Thank you for creating an account. You're now part of our trading community!</p>
        
        <p>With your REGULAR account, you can:</p>
        <ul>
            <li>View real-time cryptocurrency prices</li>
            <li>Read financial news and analysis</li>
            <li>Access basic trading charts</li>
        </ul>
        
        <p>Want more features? Upgrade to VIP for AI-powered analysis and advanced charts!</p>
        
        <p>
            <a th:href="${loginUrl}" class="button">Start Trading</a>
        </p>
        
        <p>Happy trading!</p>
    </div>
</body>
</html>
```

**File**: `user-service/src/main/resources/templates/emails/password-changed.html` (NEW)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Password Changed</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .warning { background-color: #fef3c7; padding: 15px; border-radius: 5px; }
    </style>
</head>
<body>
    <div class="container">
        <h2>Password Changed</h2>
        
        <p>Hello <span th:text="${name}">User</span>,</p>
        
        <p>Your password was successfully changed.</p>
        
        <div class="warning">
            <strong>⚠️ Security Notice:</strong>
            <p>If you didn't make this change, please contact support immediately at 
            <a th:href="'mailto:' + ${supportEmail}" th:text="${supportEmail}">support</a></p>
        </div>
        
        <p>For your security, all active sessions have been logged out.</p>
    </div>
</body>
</html>
```

**File**: `user-service/src/main/resources/templates/emails/subscription-upgrade.html` (NEW)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Subscription Upgraded</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .success { background-color: #d1fae5; padding: 15px; border-radius: 5px; margin: 20px 0; }
        .button { 
            display: inline-block; 
            padding: 12px 24px; 
            background-color: #8b5cf6; 
            color: white; 
            text-decoration: none; 
            border-radius: 5px; 
        }
    </style>
</head>
<body>
    <div class="container">
        <h2>🎉 Welcome to VIP, <span th:text="${name}">User</span>!</h2>
        
        <div class="success">
            <p><strong>Your subscription has been upgraded!</strong></p>
            <p>Plan: <span th:text="${plan}">VIP</span></p>
            <p>Duration: <span th:text="${months}">1</span> month(s)</p>
            <p>Total: $<span th:text="${price}">29.99</span></p>
        </div>
        
        <p>You now have access to:</p>
        <ul>
            <li>🤖 AI-powered market analysis</li>
            <li>📊 Advanced trading charts</li>
            <li>⚡ Priority customer support</li>
            <li>📈 Real-time price alerts</li>
        </ul>
        
        <p>
            <a th:href="${dashboardUrl}" class="button">Go to Dashboard</a>
        </p>
        
        <p>Enjoy your VIP features!</p>
    </div>
</body>
</html>
```

---

#### ✅ Task BE-EMAIL-5: Enable Async Processing

**File**: `user-service/src/main/java/org/example/userservice/UserServiceApplication.java`

```java
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  // ✅ Enable @Async for email service
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

---

#### ✅ Task BE-EMAIL-6: Integrate Email Service

**Update PasswordService to send emails:**

```java
// In PasswordService.initiatePasswordReset()
emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);

// In PasswordService.resetPassword()
emailService.sendPasswordChangedEmail(user.getEmail(), user.getFullName());
```

**Update AuthService to send welcome email:**

```java
// In AuthService.registerUser() after save
emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getFullName());
```

**Update SubscriptionService to send confirmation:**

```java
// In SubscriptionService.upgradeToVip() after save
emailService.sendSubscriptionUpgradeEmail(
    user.getEmail(), 
    user.getFullName(), 
    SubscriptionType.VIP, 
    request.getDurationMonths()
);
```

---

### 📋 Checklist for Section 7:

- [ ] BE-EMAIL-1: Add Spring Mail and Thymeleaf dependencies
- [ ] BE-EMAIL-2: Configure Gmail SMTP in application.yml
- [ ] BE-EMAIL-3: Create EmailService with async methods
- [ ] BE-EMAIL-4: Create HTML email templates
- [ ] BE-EMAIL-5: Enable @EnableAsync
- [ ] BE-EMAIL-6: Integrate email service into auth flows
- [ ] BE-EMAIL-7: Set up Gmail App Password
- [ ] BE-EMAIL-8: Test password reset email
- [ ] BE-EMAIL-9: Test welcome email on registration
- [ ] BE-EMAIL-10: Test subscription upgrade email

**Estimated Time**: 3-4 hours

**Gmail Setup Time**: 15 minutes

---

## 8. 🟢 LOW: Mock Payment Gateway

**Priority**: LOW | **Effort**: 2-3 hours (architecture planning only)

### Current Approach:

The `SubscriptionService` already has a **mock payment method** that always succeeds:

```java
private boolean mockProcessPayment(User user, UpgradeSubscriptionRequest request) {
    double totalAmount = SubscriptionType.VIP.getMonthlyPrice() * request.getDurationMonths();
    log.info("MOCK PAYMENT: User {} charged ${} for {} months VIP", 
        user.getEmail(), totalAmount, request.getDurationMonths());
    return true; // Always succeed
}
```

### Proposed Architecture for Future Real Payment Integration:

```
┌─────────────────────────────────────────────────────────────┐
│                    Payment Gateway Options                   │
├─────────────────────────────────────────────────────────────┤
│ 1. Stripe (Recommended)                                     │
│    - Pros: Best developer experience, extensive docs        │
│    - Cons: 2.9% + $0.30 per transaction                    │
│    - Setup: stripe.com → Get API keys                      │
│                                                             │
│ 2. PayPal                                                   │
│    - Pros: Users trust PayPal, no card entry needed        │
│    - Cons: Higher fees (3.49% + $0.49)                     │
│    - Setup: developer.paypal.com → Get credentials         │
│                                                             │
│ 3. Square                                                   │
│    - Pros: Simple API, good for small businesses           │
│    - Cons: Limited international support                   │
│    - Setup: developer.squareup.com → Get access token      │
└─────────────────────────────────────────────────────────────┘
```

### Implementation Architecture:

#### Option 1: Direct Integration (Recommended for learning)

```java
// PaymentService.java
@Service
public class PaymentService {
    private final StripeService stripeService;
    private final PayPalService paypalService;
    
    public PaymentResult processPayment(
        PaymentMethod method,   // STRIPE, PAYPAL, MOCK
        double amount,
        String currency,
        String userId
    ) {
        switch (method) {
            case STRIPE:
                return stripeService.charge(amount, currency, userId);
            case PAYPAL:
                return paypalService.charge(amount, currency, userId);
            case MOCK:
            default:
                return mockPayment(amount, currency, userId);
        }
    }
    
    private PaymentResult mockPayment(double amount, String currency, String userId) {
        log.info("MOCK: Charged {} {} to user {}", amount, currency, userId);
        return PaymentResult.success("MOCK_TXN_" + UUID.randomUUID());
    }
}
```

#### Option 2: Webhook-Based (Production-ready)

```java
/**
 * Stripe Webhook Handler
 * Stripe sends POST request when payment succeeds/fails
 */
@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {
    
    @PostMapping
    public ResponseEntity<?> handleStripeWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        // 1. Verify webhook signature
        // 2. Parse event type (payment_intent.succeeded, etc.)
        // 3. Update user subscription in database
        // 4. Send confirmation email
        
        return ResponseEntity.ok().build();
    }
}
```

### Database Schema Changes Needed:

```java
// Payment.java (NEW)
@Document(collection = "payments")
@Data
@Builder
public class Payment {
    @Id
    private String id;
    
    private String userId;
    private String transactionId;      // From payment gateway
    private PaymentMethod method;       // STRIPE, PAYPAL, MOCK
    private PaymentStatus status;       // PENDING, SUCCESS, FAILED, REFUNDED
    
    private Double amount;
    private String currency;
    
    private SubscriptionType subscriptionType;
    private Integer durationMonths;
    
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    private String errorMessage; // If failed
}
```

### Stripe Integration Example:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.1.0</version>
</dependency>
```

```java
// StripeService.java
@Service
public class StripeService {
    
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
    
    public PaymentResult charge(double amount, String currency, String userId) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100)) // Stripe uses cents
                .setCurrency(currency)
                .putMetadata("userId", userId)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();
            
            PaymentIntent intent = PaymentIntent.create(params);
            
            return PaymentResult.success(intent.getId());
        } catch (StripeException e) {
            return PaymentResult.failure(e.getMessage());
        }
    }
}
```

### Configuration:

```yaml
# application.yml
stripe:
  api:
    key: ${STRIPE_SECRET_KEY}
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET}

paypal:
  client:
    id: ${PAYPAL_CLIENT_ID}
    secret: ${PAYPAL_CLIENT_SECRET}
  mode: sandbox  # or 'live' for production
```

### Frontend Integration Flow:

```typescript
// Stripe Checkout Flow
async function handleVipUpgrade() {
  // 1. Backend creates PaymentIntent
  const { clientSecret } = await api.post('/subscription/create-payment-intent', {
    plan: 'VIP',
    months: 1
  });
  
  // 2. Frontend shows Stripe Elements form
  const stripe = await loadStripe(STRIPE_PUBLISHABLE_KEY);
  const { error } = await stripe.confirmPayment({
    clientSecret,
    confirmParams: {
      return_url: 'http://localhost:3000/subscription/success'
    }
  });
  
  // 3. Stripe redirects to success page
  // 4. Webhook updates subscription in background
}
```

### 📋 Checklist (LOW PRIORITY):

- [ ] BE-PAY-1: Create Payment entity and repository
- [ ] BE-PAY-2: Create PaymentService interface
- [ ] BE-PAY-3: Implement StripeService
- [ ] BE-PAY-4: Implement PayPalService (optional)
- [ ] BE-PAY-5: Create webhook endpoints
- [ ] BE-PAY-6: Add payment history to user profile
- [ ] BE-PAY-7: Test with Stripe test mode
- [ ] BE-PAY-8: Add refund functionality

**Estimated Time**: 2-3 hours (architecture), 8-12 hours (full implementation)

**Note**: Keep mock payment for now. Real payment can be added later without changing subscription logic.

---

## 9. Testing Requirements

**Priority**: HIGH | **Effort**: 6-8 hours

### Unit Tests Needed:

```java
// SubscriptionServiceTest.java
@Test
void shouldUpgradeRegularToVip() {
    // Given: User with REGULAR subscription
    // When: upgradeToVip() called
    // Then: User has VIP subscription with correct expiry date
}

@Test
void shouldNotAllowUpgradeIfAlreadyVip() {
    // Given: User already has VIP
    // When: upgradeToVip() called
    // Then: Throws IllegalArgumentException
}

// OAuth2ServiceTest.java
@Test
void shouldCreateNewUserFromGoogleOAuth() {
    // Given: Valid Google ID token for new user
    // When: processGoogleLogin() called
    // Then: New user created with email verified = true
}

// PasswordServiceTest.java
@Test
void shouldGenerateResetTokenAndSendEmail() {
    // Given: Existing user email
    // When: initiatePasswordReset() called
    // Then: Token saved to DB and email sent
}

@Test
void shouldRejectExpiredResetToken() {
    // Given: Reset token from 2 hours ago
    // When: resetPassword() called
    // Then: Throws InvalidTokenException
}

// JwtServiceTest.java
@Test
void shouldIncludeRolesInJWT() {
    // Given: VIP user
    // When: generateAccessToken() called
    // Then: JWT contains ROLE_USER and ROLE_VIP
}
```

### Integration Tests:

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    
    @Test
    void shouldCompleteOAuthFlowEndToEnd() {
        // 1. POST /api/auth/oauth2/google with valid token
        // 2. Verify JWT returned
        // 3. Use JWT to access protected endpoint
    }
    
    @Test
    void shouldCompletePasswordResetFlow() {
        // 1. POST /api/auth/password/forgot
        // 2. Extract token from email (mock)
        // 3. POST /api/auth/password/reset with token
        // 4. Login with new password
    }
    
    @Test
    void shouldUpgradeAndAccessVipEndpoint() {
        // 1. Register as REGULAR user
        // 2. Try to access VIP endpoint → 403
        // 3. Upgrade to VIP
        // 4. Access VIP endpoint → 200
    }
}
```

### 📋 Testing Checklist:

- [ ] BE-TEST-1: Unit tests for SubscriptionService
- [ ] BE-TEST-2: Unit tests for OAuth2Service
- [ ] BE-TEST-3: Unit tests for PasswordService
- [ ] BE-TEST-4: Unit tests for JwtService (roles)
- [ ] BE-TEST-5: Integration test for OAuth flow
- [ ] BE-TEST-6: Integration test for password reset
- [ ] BE-TEST-7: Integration test for RBAC (VIP-only endpoints)
- [ ] BE-TEST-8: Test account lockout after 5 failed logins
- [ ] BE-TEST-9: Test email service (mock SMTP)
- [ ] BE-TEST-10: Achieve 70%+ code coverage

---

## Priority Matrix & Timeline

### Week 1 (January 20-24, 2026):

| Day | Priority | Tasks | Hours |
|-----|----------|-------|-------|
| Mon | 🔴 CRITICAL | BE-SUB (Subscription System) | 4-5h |
| Tue | 🔴 CRITICAL | BE-OAUTH (Google Sign-In) | 6-8h |
| Wed | 🔴 CRITICAL | BE-PWD (Password Recovery) | 5-6h |
| Thu | 🟡 HIGH | BE-PROFILE (Profile Management) | 3-4h |
| Fri | 🟡 HIGH | BE-EMAIL (Gmail SMTP Setup) | 3-4h |

### Week 2 (January 25-29, 2026):

| Day | Priority | Tasks | Hours |
|-----|----------|-------|-------|
| Mon | 🟡 HIGH | BE-RBAC (Role-Based Access) | 5-6h |
| Tue | 🔴 CRITICAL | BE-SEC (Security Fixes) | 4-5h |
| Wed-Thu | Testing | BE-TEST (All tests) | 6-8h |
| Fri | Documentation | Update OpenAPI, README | 2-3h |

**Total Estimated Effort**: 45-55 hours

---

## Dependencies & Prerequisites

### Environment Variables Required:

```bash
# micro.env (add these)

# Google OAuth 2.0
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret

# Email Service (Gmail SMTP)
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-16-char-app-password

# Frontend URL
FRONTEND_URL=http://localhost:3000

# Redis (for rate limiting)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### External Services Setup:

1. **Google Cloud Console** (~30 min)
   - Create OAuth 2.0 credentials
   - Configure redirect URIs

2. **Gmail App Password** (~15 min)
   - Enable 2-Step Verification
   - Generate App Password

3. **Redis** (already in docker-compose.yml)
   - Used for rate limiting
   - Already configured

---

## Success Criteria

✅ **Phase Complete When:**

- [ ] Users can register and choose REGULAR or VIP
- [ ] Users can sign in with Google OAuth
- [ ] Users can reset forgotten passwords via email
- [ ] Users can update their profile
- [ ] VIP users have access to VIP-only endpoints
- [ ] Regular users get 403 on VIP endpoints
- [ ] JWT tokens include roles
- [ ] Account locks after 5 failed logins
- [ ] Rate limiting works on login endpoint
- [ ] All emails send successfully
- [ ] 70%+ test coverage
- [ ] OpenAPI docs updated

---

**END OF BACKEND TODO PART 3**
