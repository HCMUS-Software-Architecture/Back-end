# Phase 1: Monolithic Foundation - Implementation Guide

**Target Scale**: 1-10 users  
**Architecture**: Single Spring Boot application with embedded services  
**Duration**: Weeks 1-2 (November 30 - December 13, 2025)

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Setup](#project-setup)
4. [Core Components Implementation](#core-components-implementation)
5. [Database Schema](#database-schema)
6. [API Development](#api-development)
7. [Frontend Scaffold](#frontend-scaffold)
8. [Deployment](#deployment)
9. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
10. [References](#references)

---

## Overview

### Goals

- Set up a functional monolithic Spring Boot application
- Implement basic REST API skeleton
- Create PostgreSQL database with JSONB support for flexible schemas
- Implement basic Crawler module for 2 news sources
- Set up Next.js frontend scaffold
- Enable scheduled crawling

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
              │  (All data)   │
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
| E1.6 | Frontend Scaffold | Medium |

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

## Core Components Implementation

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
- [jsoup Documentation](https://jsoup.org/)
- [Next.js Documentation](https://nextjs.org/docs)

### Related Project Documents

- [Architecture.md](../Architecture.md) - System architecture overview
- [CoreRequirements.md](../CoreRequirements.md) - Business requirements
- [Features.md](../Features.md) - Feature specifications
- [Phase1-TestingGuide.md](./Phase1-TestingGuide.md) - Testing strategies for Phase 1

### GitHub Resources

- [Spring Boot Examples](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-tests)
- [jsoup Examples](https://github.com/jhy/jsoup/tree/master/src/test/java/org/jsoup/examples)
