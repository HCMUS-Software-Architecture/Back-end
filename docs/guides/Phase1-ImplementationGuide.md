# Phase 1: Monolithic Foundation - Implementation Guide

**Target Scale**: 1-10 users  
**Architecture**: Single Spring Boot application with embedded services  
**Duration**: Weeks 1-2 (November 30 - December 13, 2025)

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Environment Setup](#environment-setup)
4. [Project Setup](#project-setup)
5. [Database Setup](#database-setup)
6. [Core Components Implementation](#core-components-implementation)
7. [Database Schema](#database-schema)
8. [API Development](#api-development)
9. [Frontend Scaffold](#frontend-scaffold)
10. [UI/UX Implementation](#uiux-implementation)
11. [Deployment](#deployment)
12. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
13. [References](#references)

---

## Overview

### Goals

- Set up a functional monolithic Spring Boot application
- Implement basic REST API skeleton
- Create PostgreSQL database with JSONB support for flexible schemas
- Implement basic Crawler module for 2 news sources
- Set up Next.js frontend scaffold with TailwindCSS and shadcn/ui
- Enable scheduled crawling

### Core Requirements Reference

This phase implements foundational requirements from [CoreRequirements.md](core/CoreRequirements.md):

1. **Financial News Collection** - Basic crawler for 2 news sources
2. **Price Chart Display** - Data models for price storage
3. **Account Management** - User entity foundation

### Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│                 Spring Boot App                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ REST API │ │ Crawler  │ │ Price Collector  │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
│  ┌──────────┐ ┌──────────┐                      │
│  │ WebSocket│ │   NLP    │                      │
│  └──────────┘ └──────────┘                      │
└─────────────────────────────────────────────────┘
                      │
              ┌───────┴───────┐
              │  PostgreSQL   │
              │  (Dockerized) │
              └───────────────┘
```

### Epics for Phase 1

| Epic | Description | Priority |
|------|-------------|----------|
| E1.1 | Project Structure & Configuration | High |
| E1.2 | Database Setup & Entity Models | High |
| E1.3 | REST API Skeleton | High |
| E1.4 | Basic Crawler Module | High |
| E1.5 | Scheduled Crawling | Medium |
| E1.6 | Frontend Scaffold with UI/UX Foundation | Medium |

---

## Prerequisites

### Required Software

Ensure the following are installed:

| Software | Version | Installation Guide |
|----------|---------|-------------------|
| Java JDK | 17+ | [AdoptOpenJDK](https://adoptopenjdk.net/) |
| Maven | 3.8+ | [Maven Install](https://maven.apache.org/install.html) |
| Docker Desktop | 20.10+ | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| Node.js | 18+ | [Node.js](https://nodejs.org/) |
| Git | 2.30+ | [Git SCM](https://git-scm.com/) |

### Verify Installation

**Bash (Linux/macOS):**
```bash
java -version
mvn -version
docker --version
docker compose version
node --version
npm --version
```

**PowerShell (Windows 10/11):**
```powershell
java -version
mvn -version
docker --version
docker compose version
node --version
npm --version
```

---

## Environment Setup

### Step 1: Set Up Development Environment

This phase uses a **Dockerized PostgreSQL database** to ensure consistent development environments across all team members. No local database installation is required—only Docker Desktop.

### Step 2: Create Environment Variables

Create a `.env` file in the project root (add to `.gitignore`):

```bash
# Database Configuration
POSTGRES_DB=trading
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

# Crawler Configuration
CRAWLER_ENABLED=true
CRAWLER_INITIAL_DELAY=60000
CRAWLER_FIXED_DELAY=300000
```

### Step 3: Docker Network Setup

**Bash (Linux/macOS):**
```bash
# Create Docker network for trading platform services
docker network create trading-network

# Verify network
docker network ls | grep trading
```

**PowerShell (Windows 10/11):**
```powershell
# Create Docker network for trading platform services
docker network create trading-network

# Verify network
docker network ls | Select-String "trading"
```

---

## Project Setup

### Step 1: Clone Repository

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

### Step 2: Create Project Structure

Create the following directory structure:

**Bash (Linux/macOS):**
```bash
mkdir -p src/main/java/com/example/backend/{config,controller,service,repository,model,dto,crawler}
mkdir -p src/main/resources
mkdir -p src/test/java/com/example/backend
mkdir -p docker
```

**PowerShell (Windows 10/11):**
```powershell
New-Item -ItemType Directory -Force -Path src/main/java/com/example/backend/config
New-Item -ItemType Directory -Force -Path src/main/java/com/example/backend/controller
New-Item -ItemType Directory -Force -Path src/main/java/com/example/backend/service
New-Item -ItemType Directory -Force -Path src/main/java/com/example/backend/repository
New-Item -ItemType Directory -Force -Path src/main/java/com/example/backend/model
New-Item -ItemType Directory -Force -Path src/main/java/com/example/backend/dto
New-Item -ItemType Directory -Force -Path src/main/java/com/example/backend/crawler
New-Item -ItemType Directory -Force -Path src/main/resources
New-Item -ItemType Directory -Force -Path src/test/java/com/example/backend
New-Item -ItemType Directory -Force -Path docker
```

### Step 3: Configure Maven Dependencies

Update `pom.xml` with required dependencies:

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Web Parsing -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.17.2</version>
    </dependency>
    
    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- DevTools -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Step 4: Create Docker Compose Configuration

Create `docker/docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: trading-postgres
    environment:
      POSTGRES_DB: trading
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

Create `docker/init-db.sql`:

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create articles table with JSONB
CREATE TABLE IF NOT EXISTS articles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    url VARCHAR(2048) NOT NULL UNIQUE,
    title VARCHAR(1024),
    body TEXT,
    source VARCHAR(255),
    published_at TIMESTAMP,
    crawled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    raw_html TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on URL for deduplication
CREATE INDEX IF NOT EXISTS idx_articles_url ON articles(url);
CREATE INDEX IF NOT EXISTS idx_articles_source ON articles(source);
CREATE INDEX IF NOT EXISTS idx_articles_published_at ON articles(published_at);
CREATE INDEX IF NOT EXISTS idx_articles_metadata ON articles USING GIN(metadata);
```

### Step 5: Start Development Environment

**Bash (Linux/macOS):**
```bash
# Navigate to docker directory
cd docker

# Start PostgreSQL
docker compose up -d

# Verify PostgreSQL is running
docker compose ps
docker compose logs postgres

# Return to project root
cd ..
```

**PowerShell (Windows 10/11):**
```powershell
# Navigate to docker directory
Set-Location docker

# Start PostgreSQL
docker compose up -d

# Verify PostgreSQL is running
docker compose ps
docker compose logs postgres

# Return to project root
Set-Location ..
```

---

## Database Setup

This section provides comprehensive database setup following the [DatabaseDesign.md](core/DatabaseDesign.md) specifications.

### Database Strategy for Phase 1

In Phase 1, we use a **single PostgreSQL database** with JSONB support for flexible schemas:

```
┌─────────────────────────────────────────────────────┐
│                    PostgreSQL                        │
│  ┌─────────────┐ ┌─────────────┐ ┌───────────────┐  │
│  │   articles  │ │price_candles│ │    users      │  │
│  │   (JSONB)   │ │             │ │               │  │
│  └─────────────┘ └─────────────┘ └───────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Step 1: Initialize Database Schema

Update `docker/init-db.sql` with the comprehensive schema:

```sql
-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- SCHEMA: auth (User Management)
-- Reference: DatabaseDesign.md - Users & Authentication
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS auth;

-- Users table
CREATE TABLE auth.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    role            VARCHAR(20) NOT NULL DEFAULT 'TRADER',
    email_verified  BOOLEAN DEFAULT FALSE,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,
    
    CONSTRAINT chk_role CHECK (role IN ('TRADER', 'ANALYST', 'ADMIN'))
);

-- User preferences
CREATE TABLE auth.user_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    theme           VARCHAR(20) DEFAULT 'dark',
    default_symbol  VARCHAR(20) DEFAULT 'BTCUSDT',
    default_interval VARCHAR(10) DEFAULT '1h',
    timezone        VARCHAR(50) DEFAULT 'UTC',
    notifications   JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT unique_user_prefs UNIQUE (user_id)
);

-- Indexes for auth schema
CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_users_role ON auth.users(role);

-- =============================================================================
-- SCHEMA: trading (Price Data)
-- Reference: DatabaseDesign.md - Trading Symbols & Price Data
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS trading;

-- Symbols table
CREATE TABLE trading.symbols (
    id              VARCHAR(20) PRIMARY KEY,  -- e.g., 'BTCUSDT'
    base_asset      VARCHAR(10) NOT NULL,     -- e.g., 'BTC'
    quote_asset     VARCHAR(10) NOT NULL,     -- e.g., 'USDT'
    exchange        VARCHAR(50) NOT NULL DEFAULT 'binance',
    precision_price INT NOT NULL DEFAULT 2,
    precision_qty   INT NOT NULL DEFAULT 8,
    min_qty         DECIMAL(20, 8) DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Price candles table (optimized for time-series queries)
CREATE TABLE trading.price_candles (
    id              BIGSERIAL PRIMARY KEY,
    symbol_id       VARCHAR(20) NOT NULL REFERENCES trading.symbols(id),
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
    CONSTRAINT unique_candle UNIQUE (symbol_id, interval, open_time)
);

-- Indexes for trading schema
CREATE INDEX idx_symbols_exchange ON trading.symbols(exchange);
CREATE INDEX idx_symbols_active ON trading.symbols(is_active);
CREATE INDEX idx_candles_symbol_interval_time ON trading.price_candles(symbol_id, interval, open_time DESC);
CREATE INDEX idx_candles_time ON trading.price_candles(open_time DESC);

-- =============================================================================
-- SCHEMA: content (Articles - Phase 1 with JSONB)
-- Reference: DatabaseDesign.md - Articles (Phase 1 with JSONB)
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS content;

CREATE TABLE content.articles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url             TEXT NOT NULL UNIQUE,
    url_hash        VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 for dedup
    title           TEXT NOT NULL,
    body            TEXT,
    source_name     VARCHAR(100) NOT NULL,
    published_at    TIMESTAMPTZ,
    crawled_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    raw_data        JSONB,  -- Raw HTML, metadata
    symbols         VARCHAR(20)[] DEFAULT '{}',
    status          VARCHAR(20) DEFAULT 'RAW',
    
    CONSTRAINT chk_status CHECK (status IN ('RAW', 'NORMALIZED', 'ANALYZED', 'FAILED'))
);

-- Indexes for content schema
CREATE INDEX idx_articles_url_hash ON content.articles(url_hash);
CREATE INDEX idx_articles_source ON content.articles(source_name);
CREATE INDEX idx_articles_published ON content.articles(published_at DESC);
CREATE INDEX idx_articles_symbols ON content.articles USING GIN(symbols);
CREATE INDEX idx_articles_status ON content.articles(status);

-- =============================================================================
-- Initial Data: Default Symbols
-- =============================================================================
INSERT INTO trading.symbols (id, base_asset, quote_asset, exchange) VALUES
    ('BTCUSDT', 'BTC', 'USDT', 'binance'),
    ('ETHUSDT', 'ETH', 'USDT', 'binance'),
    ('BNBUSDT', 'BNB', 'USDT', 'binance')
ON CONFLICT (id) DO NOTHING;
```

### Step 2: Verify Database Setup

**Bash (Linux/macOS):**
```bash
# Connect to PostgreSQL and verify schemas
docker exec -it trading-postgres psql -U postgres -d trading -c "\dn"

# List tables in each schema
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt auth.*"
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt trading.*"
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt content.*"

# Verify initial data
docker exec -it trading-postgres psql -U postgres -d trading -c "SELECT * FROM trading.symbols"
```

**PowerShell (Windows 10/11):**
```powershell
# Connect to PostgreSQL and verify schemas
docker exec -it trading-postgres psql -U postgres -d trading -c "\dn"

# List tables in each schema
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt auth.*"
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt trading.*"
docker exec -it trading-postgres psql -U postgres -d trading -c "\dt content.*"

# Verify initial data
docker exec -it trading-postgres psql -U postgres -d trading -c "SELECT * FROM trading.symbols"
```

### Step 3: Database Administration (Optional)

For database administration, you can use these Docker-based tools instead of installing local database management software:

**Option A: pgAdmin (Web-based)**
```yaml
# Add to docker/docker-compose.yml
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: trading-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@trading.local
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres
```

Access pgAdmin at: http://localhost:5050

**Option B: Command-line with psql**
```bash
# Interactive PostgreSQL shell
docker exec -it trading-postgres psql -U postgres -d trading
```

### Step 1: Application Configuration

Create `src/main/resources/application.properties`:

```properties
# Application
spring.application.name=trading-platform
server.port=8080

# Database (H2 for local dev, PostgreSQL for dev profile)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

Create `src/main/resources/application-dev.properties`:

```properties
# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/trading
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driverClassName=org.postgresql.Driver

# JPA for PostgreSQL
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update

# Disable H2 console
spring.h2.console.enabled=false

# Crawler Configuration
crawler.enabled=true
crawler.initial-delay=60000
crawler.fixed-delay=300000
```

### Step 2: Create Entity Models

Create `src/main/java/com/example/backend/model/Article.java`:

```java
package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 2048)
    private String url;
    
    @Column(length = 1024)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Column(length = 255)
    private String source;
    
    private LocalDateTime publishedAt;
    
    private LocalDateTime crawledAt;
    
    @Column(columnDefinition = "TEXT")
    private String rawHtml;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        crawledAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Step 3: Create Repository

Create `src/main/java/com/example/backend/repository/ArticleRepository.java`:

```java
package com.example.backend.repository;

import com.example.backend.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    
    Optional<Article> findByUrl(String url);
    
    boolean existsByUrl(String url);
    
    Page<Article> findBySource(String source, Pageable pageable);
    
    Page<Article> findByPublishedAtBetween(
        LocalDateTime start, 
        LocalDateTime end, 
        Pageable pageable
    );
    
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.body LIKE %:keyword%")
    Page<Article> searchByKeyword(String keyword, Pageable pageable);
}
```

### Step 4: Create Service Layer

Create `src/main/java/com/example/backend/service/ArticleService.java`:

```java
package com.example.backend.service;

import com.example.backend.model.Article;
import com.example.backend.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    
    public Page<Article> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }
    
    public Optional<Article> getArticleById(UUID id) {
        return articleRepository.findById(id);
    }
    
    public Optional<Article> getArticleByUrl(String url) {
        return articleRepository.findByUrl(url);
    }
    
    @Transactional
    public Article saveArticle(Article article) {
        // Check for duplicate
        if (articleRepository.existsByUrl(article.getUrl())) {
            log.info("Article already exists: {}", article.getUrl());
            return articleRepository.findByUrl(article.getUrl()).orElse(null);
        }
        return articleRepository.save(article);
    }
    
    public Page<Article> getArticlesBySource(String source, Pageable pageable) {
        return articleRepository.findBySource(source, pageable);
    }
    
    public Page<Article> searchArticles(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable);
    }
}
```

### Step 5: Create REST Controller

Create `src/main/java/com/example/backend/controller/ArticleController.java`:

```java
package com.example.backend.controller;

import com.example.backend.model.Article;
import com.example.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    
    @GetMapping
    public ResponseEntity<Page<Article>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(articleService.getAllArticles(pageRequest));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticleById(@PathVariable UUID id) {
        return articleService.getArticleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/source/{source}")
    public ResponseEntity<Page<Article>> getArticlesBySource(
            @PathVariable String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ResponseEntity.ok(articleService.getArticlesBySource(source, pageRequest));
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<Article>> searchArticles(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ResponseEntity.ok(articleService.searchArticles(q, pageRequest));
    }
}
```

Create `src/main/java/com/example/backend/controller/HealthController.java`:

```java
package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString(),
            "service", "trading-platform-api"
        ));
    }
    
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        return ResponseEntity.ok(Map.of(
            "version", "1.0.0-SNAPSHOT",
            "phase", "1"
        ));
    }
}
```

### Step 6: Implement Basic Crawler

Create `src/main/java/com/example/backend/crawler/CrawlerService.java`:

```java
package com.example.backend.crawler;

import com.example.backend.model.Article;
import com.example.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {
    
    private final ArticleService articleService;
    
    // Source configurations (can be externalized)
    private static final Map<String, SourceConfig> SOURCES = Map.of(
        "coindesk", new SourceConfig(
            "https://www.coindesk.com/markets/",
            "article.article-card",
            "h4, h3, .headline",
            ".article-body, .content"
        ),
        "cointelegraph", new SourceConfig(
            "https://cointelegraph.com/news",
            "article.post-card",
            ".post-card__title",
            ".post-content"
        )
    );
    
    @Scheduled(fixedDelayString = "${crawler.fixed-delay:300000}", 
               initialDelayString = "${crawler.initial-delay:60000}")
    public void scheduledCrawl() {
        log.info("Starting scheduled crawl at {}", LocalDateTime.now());
        SOURCES.forEach((name, config) -> {
            try {
                crawlSource(name, config);
            } catch (Exception e) {
                log.error("Failed to crawl source: {}", name, e);
            }
        });
        log.info("Completed scheduled crawl at {}", LocalDateTime.now());
    }
    
    public void crawlSource(String sourceName, SourceConfig config) {
        try {
            log.info("Crawling source: {}", sourceName);
            
            Document doc = Jsoup.connect(config.baseUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();
            
            Elements articles = doc.select(config.articleSelector());
            log.info("Found {} articles from {}", articles.size(), sourceName);
            
            for (Element articleElement : articles) {
                try {
                    processArticleElement(articleElement, sourceName, config);
                } catch (Exception e) {
                    log.warn("Failed to process article element from {}: {}", sourceName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error crawling {}: {}", sourceName, e.getMessage());
        }
    }
    
    private void processArticleElement(Element element, String source, SourceConfig config) {
        // Extract URL
        Element link = element.selectFirst("a[href]");
        if (link == null) return;
        
        String url = link.absUrl("href");
        if (url.isEmpty()) return;
        
        // Extract title
        Element titleElement = element.selectFirst(config.titleSelector());
        String title = titleElement != null ? titleElement.text() : "";
        
        // Build article
        Article article = Article.builder()
                .url(url)
                .title(title)
                .source(source)
                .rawHtml(element.outerHtml())
                .metadata(new HashMap<>(Map.of(
                    "crawledBy", "basic-crawler",
                    "extractedAt", LocalDateTime.now().toString()
                )))
                .build();
        
        // Save (with deduplication)
        Article saved = articleService.saveArticle(article);
        if (saved != null && saved.getId() != null) {
            log.debug("Saved article: {}", title);
        }
    }
    
    public record SourceConfig(
        String baseUrl,
        String articleSelector,
        String titleSelector,
        String bodySelector
    ) {}
}
```

Create `src/main/java/com/example/backend/config/SchedulingConfig.java`:

```java
package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

---

## Database Schema

### Core Tables

| Table | Purpose | Columns |
|-------|---------|---------|
| `articles` | Store crawled news articles | id, url, title, body, source, published_at, metadata (JSONB) |

### JSONB Metadata Structure

```json
{
  "crawledBy": "basic-crawler",
  "extractedAt": "2025-12-01T10:00:00",
  "symbols": ["BTC", "ETH"],
  "authors": ["John Doe"],
  "tags": ["bitcoin", "market"]
}
```

---

## API Development

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `GET /api/health` | GET | Health check |
| `GET /api/version` | GET | API version info |
| `GET /api/articles` | GET | List articles (paginated) |
| `GET /api/articles/{id}` | GET | Get article by ID |
| `GET /api/articles/source/{source}` | GET | Get articles by source |
| `GET /api/articles/search?q={keyword}` | GET | Search articles |

### Testing Endpoints

**Bash (Linux/macOS):**
```bash
# Health check
curl -X GET http://localhost:8080/api/health | jq

# Get all articles
curl -X GET "http://localhost:8080/api/articles?page=0&size=10" | jq

# Search articles
curl -X GET "http://localhost:8080/api/articles/search?q=bitcoin" | jq
```

**PowerShell (Windows 10/11):**
```powershell
# Health check
Invoke-RestMethod -Uri http://localhost:8080/api/health | ConvertTo-Json

# Get all articles
Invoke-RestMethod -Uri "http://localhost:8080/api/articles?page=0&size=10" | ConvertTo-Json

# Search articles
Invoke-RestMethod -Uri "http://localhost:8080/api/articles/search?q=bitcoin" | ConvertTo-Json
```

---

## Frontend Scaffold

### Setting Up Next.js

**Bash (Linux/macOS):**
```bash
# Create Next.js app in frontend directory
npx create-next-app@latest frontend --typescript --tailwind --eslint --app --src-dir

# Navigate to frontend
cd frontend

# Install additional dependencies
npm install axios swr @tanstack/react-query

# Start development server
npm run dev
```

**PowerShell (Windows 10/11):**
```powershell
# Create Next.js app in frontend directory
npx create-next-app@latest frontend --typescript --tailwind --eslint --app --src-dir

# Navigate to frontend
Set-Location frontend

# Install additional dependencies
npm install axios swr @tanstack/react-query

# Start development server
npm run dev
```

---

## UI/UX Implementation

This section implements the foundational UI/UX following [UIUXGuidelines.md](core/UIUXGuidelines.md).

### Design System Setup

Phase 1 establishes the design foundation for the trading platform:

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Framework** | Next.js 14+ | App Router with TypeScript |
| **Styling** | TailwindCSS | Utility-first CSS |
| **Components** | shadcn/ui | Pre-built accessible components |
| **Icons** | Lucide Icons | Consistent iconography |
| **Charts** | lightweight-charts | TradingView-style charts (Phase 2+) |

### Step 1: Install UI Dependencies

**Bash (Linux/macOS):**
```bash
cd frontend

# Initialize shadcn/ui
npx shadcn-ui@latest init

# When prompted, use these settings:
# - Style: Default
# - Base color: Slate
# - CSS variables: Yes

# Install core components
npx shadcn-ui@latest add button card table tabs badge skeleton
npx shadcn-ui@latest add dialog dropdown-menu select input

# Install icons
npm install lucide-react

# Install fonts (optional - uses CDN in example)
npm install @fontsource/inter @fontsource/jetbrains-mono
```

**PowerShell (Windows 10/11):**
```powershell
Set-Location frontend

# Initialize shadcn/ui
npx shadcn-ui@latest init

# Install core components
npx shadcn-ui@latest add button card table tabs badge skeleton
npx shadcn-ui@latest add dialog dropdown-menu select input

# Install icons
npm install lucide-react

# Install fonts
npm install @fontsource/inter @fontsource/jetbrains-mono
```

### Step 2: Configure Theme (Dark Mode Default)

Create/update `frontend/src/app/globals.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    /* Light Theme */
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --card: 0 0% 100%;
    --card-foreground: 222.2 84% 4.9%;
    --popover: 0 0% 100%;
    --popover-foreground: 222.2 84% 4.9%;
    --primary: 221.2 83.2% 53.3%;
    --primary-foreground: 210 40% 98%;
    --secondary: 210 40% 96.1%;
    --secondary-foreground: 222.2 47.4% 11.2%;
    --muted: 210 40% 96.1%;
    --muted-foreground: 215.4 16.3% 46.9%;
    --accent: 210 40% 96.1%;
    --accent-foreground: 222.2 47.4% 11.2%;
    --destructive: 0 72.2% 50.6%;
    --destructive-foreground: 210 40% 98%;
    --success: 142.1 76.2% 36.3%;
    --border: 214.3 31.8% 91.4%;
    --input: 214.3 31.8% 91.4%;
    --ring: 221.2 83.2% 53.3%;
    --radius: 0.5rem;
  }

  .dark {
    /* Dark Theme (Default for Trading) */
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    --card: 217.2 32.6% 7.5%;
    --card-foreground: 210 40% 98%;
    --popover: 222.2 84% 4.9%;
    --popover-foreground: 210 40% 98%;
    --primary: 217.2 91.2% 59.8%;
    --primary-foreground: 222.2 47.4% 11.2%;
    --secondary: 217.2 32.6% 17.5%;
    --secondary-foreground: 210 40% 98%;
    --muted: 217.2 32.6% 17.5%;
    --muted-foreground: 215 20.2% 65.1%;
    --accent: 217.2 32.6% 17.5%;
    --accent-foreground: 210 40% 98%;
    --destructive: 0 84.2% 60.2%;
    --destructive-foreground: 210 40% 98%;
    --success: 142.1 70.6% 45.3%;
    --border: 217.2 32.6% 17.5%;
    --input: 217.2 32.6% 17.5%;
    --ring: 212.7 26.8% 83.9%;
    
    /* Chart Colors */
    --chart-bullish: 142.1 70.6% 45.3%;
    --chart-bearish: 0 84.2% 60.2%;
    --chart-grid: 215 20% 20%;
  }
}

@layer base {
  * {
    @apply border-border;
  }
  body {
    @apply bg-background text-foreground;
    font-feature-settings: "rlig" 1, "calt" 1;
  }
}

/* Monospace for prices */
.font-mono {
  font-feature-settings: "tnum" 1;
}
```

### Step 3: Create Basic Layout Component

Create `frontend/src/components/layout/MainLayout.tsx`:

```tsx
import React from 'react';
import Link from 'next/link';
import { BarChart3, Newspaper, Settings, Home } from 'lucide-react';

interface MainLayoutProps {
  children: React.ReactNode;
}

export function MainLayout({ children }: MainLayoutProps) {
  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 h-14 border-b border-border bg-background/95 backdrop-blur">
        <div className="container flex h-full items-center px-4">
          <div className="flex items-center gap-2">
            <span className="text-xl font-bold text-primary">Trading Platform</span>
          </div>
          <nav className="ml-8 hidden md:flex items-center gap-6">
            <Link href="/" className="text-sm font-medium hover:text-primary">
              Dashboard
            </Link>
            <Link href="/news" className="text-sm font-medium hover:text-primary">
              News
            </Link>
            <Link href="/charts" className="text-sm font-medium hover:text-primary">
              Charts
            </Link>
          </nav>
        </div>
      </header>

      {/* Main Content */}
      <main className="container px-4 py-6">
        {children}
      </main>
    </div>
  );
}
```

### Step 4: Create News Article Card Component

Create `frontend/src/components/news/ArticleCard.tsx`:

```tsx
import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ExternalLink, Clock } from 'lucide-react';

interface ArticleCardProps {
  title: string;
  source: string;
  publishedAt?: string;
  url: string;
}

export function ArticleCard({ title, source, publishedAt, url }: ArticleCardProps) {
  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'Unknown date';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Card className="hover:bg-accent/50 transition-colors">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <Badge variant="secondary" className="text-xs">
            {source}
          </Badge>
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-muted-foreground hover:text-primary"
          >
            <ExternalLink className="h-4 w-4" />
          </a>
        </div>
      </CardHeader>
      <CardContent>
        <CardTitle className="text-base font-medium leading-snug mb-2">
          {title}
        </CardTitle>
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <Clock className="h-3 w-3" />
          <span>{formatDate(publishedAt)}</span>
        </div>
      </CardContent>
    </Card>
  );
}
```

### Step 5: Create Dashboard Page

Update `frontend/src/app/page.tsx`:

```tsx
'use client';

import React, { useEffect, useState } from 'react';
import { MainLayout } from '@/components/layout/MainLayout';
import { ArticleCard } from '@/components/news/ArticleCard';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Newspaper, RefreshCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface Article {
  id: string;
  title: string;
  source: string;
  publishedAt?: string;
  url: string;
}

export default function Dashboard() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchArticles = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/articles?page=0&size=10');
      const data = await response.json();
      setArticles(data.content || []);
    } catch (error) {
      console.error('Failed to fetch articles:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchArticles();
  }, []);

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
            <p className="text-muted-foreground">
              Latest cryptocurrency news and market updates
            </p>
          </div>
          <Button variant="outline" onClick={fetchArticles} disabled={loading}>
            <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Total Articles
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{articles.length}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Sources
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {new Set(articles.map(a => a.source)).size}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Last Updated
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">Just now</p>
            </CardContent>
          </Card>
        </div>

        {/* News Feed */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Newspaper className="h-5 w-5" />
              Latest News
            </CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {[...Array(6)].map((_, i) => (
                  <Card key={i}>
                    <CardContent className="pt-6">
                      <Skeleton className="h-4 w-20 mb-4" />
                      <Skeleton className="h-4 w-full mb-2" />
                      <Skeleton className="h-4 w-3/4" />
                    </CardContent>
                  </Card>
                ))}
              </div>
            ) : articles.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                <Newspaper className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p>No articles yet. Start the crawler to fetch news.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {articles.map((article) => (
                  <ArticleCard key={article.id} {...article} />
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </MainLayout>
  );
}
```

### Accessibility Considerations

Following WCAG 2.1 AA standards from [UIUXGuidelines.md](core/UIUXGuidelines.md):

- **Color Contrast**: All text meets minimum 4.5:1 contrast ratio
- **Focus Indicators**: Visible focus rings on interactive elements
- **Keyboard Navigation**: Full keyboard accessibility with Tab navigation
- **Screen Reader Support**: Semantic HTML and ARIA labels where needed

---

## Deployment

### Build and Run

**Bash (Linux/macOS):**
```bash
# Build the application
./mvnw clean package -DskipTests

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or run the JAR
java -jar target/back-end-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

**PowerShell (Windows 10/11):**
```powershell
# Build the application
.\mvnw.cmd clean package -DskipTests

# Run with dev profile
.\mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=dev"

# Or run the JAR
java -jar target\back-end-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## Common Pitfalls & Troubleshooting

### Issue 1: PostgreSQL Connection Refused

**Symptoms**: `Connection refused` or `Unable to acquire JDBC Connection`

**Solutions**:

**Bash (Linux/macOS):**
```bash
# Check if PostgreSQL container is running
docker compose ps

# Check container logs
docker compose logs postgres

# Restart container if needed
docker compose restart postgres
```

**PowerShell (Windows 10/11):**
```powershell
# Check if PostgreSQL container is running
docker compose ps

# Check container logs
docker compose logs postgres

# Restart container if needed
docker compose restart postgres
```

### Issue 2: Crawler Blocked by Website

**Symptoms**: `403 Forbidden` or empty response

**Solutions**:
- Add proper User-Agent header
- Implement rate limiting
- Consider using proxy rotation
- Add delays between requests

```java
Document doc = Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    .referrer("https://www.google.com")
    .timeout(30000)
    .get();
```

### Issue 3: JSONB Not Working with H2

**Symptoms**: `JSONB` type error in tests

**Solution**: Use H2 for testing with JSON string storage:

```properties
# For H2 testing
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

### Issue 4: Maven Wrapper Not Executable

**Symptoms**: `Permission denied` when running `./mvnw`

**Solution (Bash):**
```bash
chmod +x mvnw
```

**Solution (PowerShell):** Use `.\mvnw.cmd` instead of `./mvnw`

---

## References

### Official Documentation

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [jsoup Documentation](https://jsoup.org/)
- [Next.js Documentation](https://nextjs.org/docs)
- [TailwindCSS Documentation](https://tailwindcss.com/docs)
- [shadcn/ui Components](https://ui.shadcn.com/)

### Related Project Documents

- [CoreRequirements.md](core/CoreRequirements.md) - Business requirements
- [DatabaseDesign.md](core/DatabaseDesign.md) - Comprehensive database architecture
- [UIUXGuidelines.md](core/UIUXGuidelines.md) - UI/UX design guidelines
- [Phase2-ImplementationGuide.md](guides/Phase2-ImplementationGuide.md) - Next phase

### GitHub Resources

- [Spring Boot Examples](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-tests)
- [jsoup Examples](https://github.com/jhy/jsoup/tree/master/src/test/java/org/jsoup/examples)
- [TradingView Lightweight Charts](https://github.com/nicktomlin/trading-view-examples) - Chart examples
