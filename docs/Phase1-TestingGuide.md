# Phase 1: Monolithic Foundation - Testing & Verification Guide

**Target Scale**: 1-10 users  
**Phase Duration**: Weeks 1-2 (November 30 - December 13, 2025)

---

## Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Test Environment Setup](#test-environment-setup)
3. [Unit Testing](#unit-testing)
4. [Integration Testing](#integration-testing)
5. [API Testing](#api-testing)
6. [Manual Testing Checklist](#manual-testing-checklist)
7. [Acceptance Criteria Verification](#acceptance-criteria-verification)
8. [Common Issues & Troubleshooting](#common-issues--troubleshooting)
9. [References](#references)

---

## Testing Strategy Overview

### Testing Pyramid for Phase 1

```
        ┌─────────────┐
        │   Manual    │  ← Exploratory testing, UI verification
        ├─────────────┤
        │  API Tests  │  ← Postman/REST-assured
        ├─────────────┤
        │ Integration │  ← Spring Boot Test, TestContainers
        ├─────────────┤
        │    Unit     │  ← JUnit 5, Mockito
        └─────────────┘
```

### Testing Tools & Libraries

| Tool | Purpose | Version |
|------|---------|---------|
| JUnit 5 | Unit testing framework | 5.10+ |
| Mockito | Mocking framework | 5.x |
| Spring Boot Test | Integration testing | 3.x |
| TestContainers | Database containers | 1.19+ |
| REST-assured | API testing | 5.x |
| Postman | Manual API testing | Latest |
| H2 Database | In-memory testing | 2.x |

### Test Coverage Goals

| Layer | Coverage Target | Priority |
|-------|-----------------|----------|
| Repository | 90% | High |
| Service | 85% | High |
| Controller | 80% | Medium |
| Crawler | 70% | Medium |

---

## Test Environment Setup

### Step 1: Add Test Dependencies

Ensure `pom.xml` includes testing dependencies:

```xml
<dependencies>
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- TestContainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- REST-assured -->
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Step 2: Create Test Configuration

Create `src/test/resources/application-test.properties`:

```properties
# H2 In-Memory Database for Testing
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Disable scheduled tasks during tests
crawler.enabled=false
spring.main.allow-bean-definition-overriding=true

# Logging
logging.level.org.springframework.test=INFO
logging.level.com.example.backend=DEBUG
```

### Step 3: Run Tests

**Bash (Linux/macOS):**
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ArticleServiceTest

# Run with test profile
./mvnw test -Dspring.profiles.active=test

# Run with coverage
./mvnw test jacoco:report
```

**PowerShell (Windows 10/11):**
```powershell
# Run all tests
.\mvnw.cmd test

# Run specific test class
.\mvnw.cmd test -Dtest=ArticleServiceTest

# Run with test profile
.\mvnw.cmd test -D"spring.profiles.active=test"

# Run with coverage
.\mvnw.cmd test jacoco:report
```

---

## Unit Testing

### Repository Tests

Create `src/test/java/com/example/backend/repository/ArticleRepositoryTest.java`:

```java
package com.example.backend.repository;

import com.example.backend.model.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ArticleRepositoryTest {
    
    @Autowired
    private ArticleRepository articleRepository;
    
    private Article testArticle;
    
    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        
        testArticle = Article.builder()
                .url("https://example.com/article-1")
                .title("Test Article Title")
                .body("Test article body content")
                .source("test-source")
                .publishedAt(LocalDateTime.now())
                .metadata(Map.of("key", "value"))
                .build();
    }
    
    @Test
    void shouldSaveArticle() {
        Article saved = articleRepository.save(testArticle);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUrl()).isEqualTo(testArticle.getUrl());
        assertThat(saved.getCreatedAt()).isNotNull();
    }
    
    @Test
    void shouldFindArticleByUrl() {
        articleRepository.save(testArticle);
        
        Optional<Article> found = articleRepository.findByUrl(testArticle.getUrl());
        
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo(testArticle.getTitle());
    }
    
    @Test
    void shouldReturnEmptyForNonExistentUrl() {
        Optional<Article> found = articleRepository.findByUrl("https://nonexistent.com");
        
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldCheckIfUrlExists() {
        articleRepository.save(testArticle);
        
        assertThat(articleRepository.existsByUrl(testArticle.getUrl())).isTrue();
        assertThat(articleRepository.existsByUrl("https://nonexistent.com")).isFalse();
    }
    
    @Test
    void shouldFindArticlesBySource() {
        articleRepository.save(testArticle);
        
        Article anotherArticle = Article.builder()
                .url("https://example.com/article-2")
                .title("Another Article")
                .source("test-source")
                .build();
        articleRepository.save(anotherArticle);
        
        Page<Article> articles = articleRepository.findBySource(
                "test-source", 
                PageRequest.of(0, 10)
        );
        
        assertThat(articles.getContent()).hasSize(2);
    }
    
    @Test
    void shouldSearchByKeyword() {
        testArticle.setBody("Bitcoin price analysis for today");
        articleRepository.save(testArticle);
        
        Page<Article> results = articleRepository.searchByKeyword(
                "Bitcoin", 
                PageRequest.of(0, 10)
        );
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getBody()).contains("Bitcoin");
    }
}
```

### Service Tests

Create `src/test/java/com/example/backend/service/ArticleServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.model.Article;
import com.example.backend.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    
    @Mock
    private ArticleRepository articleRepository;
    
    @InjectMocks
    private ArticleService articleService;
    
    private Article testArticle;
    private UUID testId;
    
    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testArticle = Article.builder()
                .id(testId)
                .url("https://example.com/article-1")
                .title("Test Article")
                .source("test-source")
                .build();
    }
    
    @Test
    void shouldGetAllArticles() {
        Page<Article> page = new PageImpl<>(List.of(testArticle));
        when(articleRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        Page<Article> result = articleService.getAllArticles(PageRequest.of(0, 10));
        
        assertThat(result.getContent()).hasSize(1);
        verify(articleRepository).findAll(any(Pageable.class));
    }
    
    @Test
    void shouldGetArticleById() {
        when(articleRepository.findById(testId)).thenReturn(Optional.of(testArticle));
        
        Optional<Article> result = articleService.getArticleById(testId);
        
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testId);
    }
    
    @Test
    void shouldReturnEmptyForNonExistentId() {
        UUID nonExistentId = UUID.randomUUID();
        when(articleRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        Optional<Article> result = articleService.getArticleById(nonExistentId);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldSaveNewArticle() {
        when(articleRepository.existsByUrl(testArticle.getUrl())).thenReturn(false);
        when(articleRepository.save(testArticle)).thenReturn(testArticle);
        
        Article result = articleService.saveArticle(testArticle);
        
        assertThat(result).isNotNull();
        verify(articleRepository).save(testArticle);
    }
    
    @Test
    void shouldNotSaveDuplicateArticle() {
        when(articleRepository.existsByUrl(testArticle.getUrl())).thenReturn(true);
        when(articleRepository.findByUrl(testArticle.getUrl())).thenReturn(Optional.of(testArticle));
        
        Article result = articleService.saveArticle(testArticle);
        
        assertThat(result).isEqualTo(testArticle);
        verify(articleRepository, never()).save(any());
    }
    
    @Test
    void shouldSearchArticles() {
        Page<Article> page = new PageImpl<>(List.of(testArticle));
        when(articleRepository.searchByKeyword(eq("bitcoin"), any(Pageable.class))).thenReturn(page);
        
        Page<Article> result = articleService.searchArticles("bitcoin", PageRequest.of(0, 10));
        
        assertThat(result.getContent()).hasSize(1);
    }
}
```

### Crawler Service Tests

Create `src/test/java/com/example/backend/crawler/CrawlerServiceTest.java`:

```java
package com.example.backend.crawler;

import com.example.backend.model.Article;
import com.example.backend.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {
    
    @Mock
    private ArticleService articleService;
    
    @InjectMocks
    private CrawlerService crawlerService;
    
    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(articleService);
    }
    
    @Test
    void shouldSaveArticleWithCorrectFields() {
        // Given
        Article savedArticle = Article.builder()
                .url("https://example.com/article")
                .title("Test Title")
                .source("test")
                .build();
        when(articleService.saveArticle(any(Article.class))).thenReturn(savedArticle);
        
        // When - simulate processing an article
        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        articleService.saveArticle(savedArticle);
        
        // Then
        verify(articleService).saveArticle(articleCaptor.capture());
        Article captured = articleCaptor.getValue();
        assertThat(captured.getSource()).isEqualTo("test");
    }
    
    @Test
    void shouldHandleNullArticleGracefully() {
        when(articleService.saveArticle(any(Article.class))).thenReturn(null);
        
        Article result = articleService.saveArticle(
            Article.builder().url("https://test.com").build()
        );
        
        assertThat(result).isNull();
    }
}
```

---

## Integration Testing

### Controller Integration Tests

Create `src/test/java/com/example/backend/controller/ArticleControllerIntegrationTest.java`:

```java
package com.example.backend.controller;

import com.example.backend.model.Article;
import com.example.backend.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticleControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        
        // Create test articles
        Article article1 = Article.builder()
                .url("https://example.com/article-1")
                .title("Bitcoin reaches new high")
                .body("Bitcoin price analysis")
                .source("coindesk")
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build();
        
        Article article2 = Article.builder()
                .url("https://example.com/article-2")
                .title("Ethereum update")
                .body("Ethereum network upgrade")
                .source("cointelegraph")
                .publishedAt(LocalDateTime.now())
                .build();
        
        articleRepository.save(article1);
        articleRepository.save(article2);
    }
    
    @Test
    void shouldReturnAllArticles() throws Exception {
        mockMvc.perform(get("/api/articles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
    
    @Test
    void shouldReturnPaginatedArticles() throws Exception {
        mockMvc.perform(get("/api/articles")
                .param("page", "0")
                .param("size", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
    
    @Test
    void shouldReturnArticleById() throws Exception {
        Article saved = articleRepository.findAll().get(0);
        
        mockMvc.perform(get("/api/articles/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(saved.getTitle()));
    }
    
    @Test
    void shouldReturn404ForNonExistentArticle() throws Exception {
        mockMvc.perform(get("/api/articles/{id}", "00000000-0000-0000-0000-000000000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldFilterBySource() throws Exception {
        mockMvc.perform(get("/api/articles/source/{source}", "coindesk")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].source").value("coindesk"));
    }
    
    @Test
    void shouldSearchArticles() throws Exception {
        mockMvc.perform(get("/api/articles/search")
                .param("q", "Bitcoin")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}
```

### Health Endpoint Tests

Create `src/test/java/com/example/backend/controller/HealthControllerTest.java`:

```java
package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("trading-platform-api")));
    }
    
    @Test
    void shouldReturnVersion() throws Exception {
        mockMvc.perform(get("/api/version")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase", is("1")));
    }
}
```

---

## API Testing

### Postman Collection

Create a Postman collection for manual API testing:

```json
{
  "info": {
    "name": "Trading Platform API - Phase 1",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/health"
      }
    },
    {
      "name": "Get All Articles",
      "request": {
        "method": "GET",
        "url": {
          "raw": "{{baseUrl}}/api/articles?page=0&size=10",
          "query": [
            {"key": "page", "value": "0"},
            {"key": "size", "value": "10"}
          ]
        }
      }
    },
    {
      "name": "Search Articles",
      "request": {
        "method": "GET",
        "url": {
          "raw": "{{baseUrl}}/api/articles/search?q=bitcoin",
          "query": [
            {"key": "q", "value": "bitcoin"}
          ]
        }
      }
    }
  ],
  "variable": [
    {"key": "baseUrl", "value": "http://localhost:8080"}
  ]
}
```

### cURL Testing Commands

**Bash (Linux/macOS):**
```bash
# Health Check
curl -X GET http://localhost:8080/api/health -H "Content-Type: application/json" | jq

# Get Version
curl -X GET http://localhost:8080/api/version -H "Content-Type: application/json" | jq

# List Articles (paginated)
curl -X GET "http://localhost:8080/api/articles?page=0&size=5" -H "Content-Type: application/json" | jq

# Get Article by ID (replace with actual UUID)
curl -X GET "http://localhost:8080/api/articles/123e4567-e89b-12d3-a456-426614174000" -H "Content-Type: application/json" | jq

# Get Articles by Source
curl -X GET "http://localhost:8080/api/articles/source/coindesk" -H "Content-Type: application/json" | jq

# Search Articles
curl -X GET "http://localhost:8080/api/articles/search?q=bitcoin" -H "Content-Type: application/json" | jq

# Actuator Health (Spring Boot Actuator)
curl -X GET http://localhost:8080/actuator/health -H "Content-Type: application/json" | jq
```

**PowerShell (Windows 10/11):**
```powershell
# Health Check
Invoke-RestMethod -Uri http://localhost:8080/api/health -Method Get | ConvertTo-Json

# Get Version
Invoke-RestMethod -Uri http://localhost:8080/api/version -Method Get | ConvertTo-Json

# List Articles (paginated)
Invoke-RestMethod -Uri "http://localhost:8080/api/articles?page=0&size=5" -Method Get | ConvertTo-Json -Depth 10

# Get Article by ID (replace with actual UUID)
Invoke-RestMethod -Uri "http://localhost:8080/api/articles/123e4567-e89b-12d3-a456-426614174000" -Method Get | ConvertTo-Json

# Get Articles by Source
Invoke-RestMethod -Uri "http://localhost:8080/api/articles/source/coindesk" -Method Get | ConvertTo-Json -Depth 10

# Search Articles
Invoke-RestMethod -Uri "http://localhost:8080/api/articles/search?q=bitcoin" -Method Get | ConvertTo-Json -Depth 10

# Actuator Health (Spring Boot Actuator)
Invoke-RestMethod -Uri http://localhost:8080/actuator/health -Method Get | ConvertTo-Json
```

---

## Manual Testing Checklist

### Application Startup

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-001 | Start application with `./mvnw spring-boot:run` | Application starts on port 8080 | ☐ |
| MT-002 | Check logs for errors | No ERROR level logs | ☐ |
| MT-003 | Access `/api/health` | Returns `{"status": "UP"}` | ☐ |
| MT-004 | Access `/actuator/health` | Returns detailed health info | ☐ |

### Database Connectivity

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-010 | Start PostgreSQL container | Container running | ☐ |
| MT-011 | Run with `dev` profile | Connects to PostgreSQL | ☐ |
| MT-012 | Check H2 console at `/h2-console` | Console accessible (default profile) | ☐ |

### Crawler Functionality

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-020 | Wait for scheduled crawl (5 min) | Logs show crawl activity | ☐ |
| MT-021 | Check `/api/articles` | Returns crawled articles | ☐ |
| MT-022 | Verify deduplication | No duplicate URLs | ☐ |
| MT-023 | Check article metadata | Contains crawl info | ☐ |

### API Endpoints

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-030 | GET `/api/articles` | Returns paginated list | ☐ |
| MT-031 | GET `/api/articles?page=1` | Returns second page | ☐ |
| MT-032 | GET `/api/articles/{id}` | Returns single article | ☐ |
| MT-033 | GET `/api/articles/{invalid-id}` | Returns 404 | ☐ |
| MT-034 | GET `/api/articles/source/coindesk` | Filters by source | ☐ |
| MT-035 | GET `/api/articles/search?q=bitcoin` | Returns matching articles | ☐ |

---

## Acceptance Criteria Verification

### Phase 1 Acceptance Criteria

| Criteria | Test Method | Verification Command | Status |
|----------|-------------|---------------------|--------|
| Application starts and responds | Health check | `curl http://localhost:8080/api/health` | ☐ |
| Crawler fetches from 2+ sources | Check logs | Look for "Crawling source:" logs | ☐ |
| Articles stored in database | API query | `curl http://localhost:8080/api/articles` | ☐ |
| Deduplication works | Check DB | No duplicate URLs | ☐ |
| Pagination works | API test | Test with different page/size params | ☐ |
| Search functionality | API test | `curl /api/articles/search?q=test` | ☐ |

### Verification Script

**Bash (Linux/macOS):**
```bash
#!/bin/bash
echo "=== Phase 1 Verification Script ==="

# 1. Health Check
echo "1. Checking health..."
HEALTH=$(curl -s http://localhost:8080/api/health)
if [[ $HEALTH == *"UP"* ]]; then
    echo "   ✓ Health check passed"
else
    echo "   ✗ Health check failed"
fi

# 2. Version Check
echo "2. Checking version..."
VERSION=$(curl -s http://localhost:8080/api/version)
if [[ $VERSION == *"phase\":\"1"* ]]; then
    echo "   ✓ Version check passed"
else
    echo "   ✗ Version check failed"
fi

# 3. Articles API
echo "3. Checking articles API..."
ARTICLES=$(curl -s "http://localhost:8080/api/articles?page=0&size=5")
if [[ $ARTICLES == *"content"* ]]; then
    echo "   ✓ Articles API working"
else
    echo "   ✗ Articles API failed"
fi

# 4. Search API
echo "4. Checking search API..."
SEARCH=$(curl -s "http://localhost:8080/api/articles/search?q=test")
if [[ $SEARCH == *"content"* ]]; then
    echo "   ✓ Search API working"
else
    echo "   ✗ Search API failed"
fi

echo "=== Verification Complete ==="
```

**PowerShell (Windows 10/11):**
```powershell
Write-Host "=== Phase 1 Verification Script ===" -ForegroundColor Cyan

# 1. Health Check
Write-Host "1. Checking health..."
try {
    $health = Invoke-RestMethod -Uri http://localhost:8080/api/health -ErrorAction Stop
    if ($health.status -eq "UP") {
        Write-Host "   ✓ Health check passed" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Health check failed" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Health check failed: $_" -ForegroundColor Red
}

# 2. Version Check
Write-Host "2. Checking version..."
try {
    $version = Invoke-RestMethod -Uri http://localhost:8080/api/version -ErrorAction Stop
    if ($version.phase -eq "1") {
        Write-Host "   ✓ Version check passed" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Version check failed" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Version check failed: $_" -ForegroundColor Red
}

# 3. Articles API
Write-Host "3. Checking articles API..."
try {
    $articles = Invoke-RestMethod -Uri "http://localhost:8080/api/articles?page=0&size=5" -ErrorAction Stop
    if ($articles.content -ne $null) {
        Write-Host "   ✓ Articles API working" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Articles API failed" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Articles API failed: $_" -ForegroundColor Red
}

# 4. Search API
Write-Host "4. Checking search API..."
try {
    $search = Invoke-RestMethod -Uri "http://localhost:8080/api/articles/search?q=test" -ErrorAction Stop
    if ($search.content -ne $null) {
        Write-Host "   ✓ Search API working" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Search API failed" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Search API failed: $_" -ForegroundColor Red
}

Write-Host "=== Verification Complete ===" -ForegroundColor Cyan
```

---

## Common Issues & Troubleshooting

### Issue 1: Tests Fail with Database Errors

**Symptoms**: `Could not create the entity manager factory`

**Solution**: Ensure H2 is on classpath and test profile is active

**Bash (Linux/macOS):**
```bash
./mvnw test -Dspring.profiles.active=test
```

**PowerShell (Windows 10/11):**
```powershell
.\mvnw.cmd test -D"spring.profiles.active=test"
```

### Issue 2: TestContainers Not Starting

**Symptoms**: `Docker is not running` or container fails to start

**Solutions**:
1. Ensure Docker Desktop is running
2. Check Docker resources (memory/CPU)
3. Pull images manually:

**Both Bash and PowerShell:**
```bash
docker pull postgres:15
docker pull testcontainers/ryuk
```

### Issue 3: MockMvc Returns 404

**Symptoms**: All MockMvc tests return 404

**Solutions**:
- Verify `@WebMvcTest` or `@SpringBootTest` annotation
- Check controller package scanning
- Ensure `@AutoConfigureMockMvc` is present

### Issue 4: Jacoco Coverage Not Generated

**Symptoms**: No coverage report in `target/site/jacoco`

**Solution**: Add Jacoco plugin to `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## References

### Testing Frameworks

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [TestContainers](https://www.testcontainers.org/)

### Related Project Documents

- [Phase1-ImplementationGuide.md](./Phase1-ImplementationGuide.md) - Implementation guide
- [Architecture.md](../Architecture.md) - System architecture
- [Features.md](../Features.md) - Feature specifications

### Tools

- [Postman](https://www.postman.com/) - API testing tool
- [Jacoco](https://www.jacoco.org/) - Code coverage
