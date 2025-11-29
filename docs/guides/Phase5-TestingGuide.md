# Phase 5: Microservices Preparation - Testing & Verification Guide

**Target Scale**: 10,000+ users  
**Phase Duration**: Week 9 (January 25 - January 31, 2026)

---

## Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Container Testing](#container-testing)
3. [API Documentation Testing](#api-documentation-testing)
4. [Performance Testing](#performance-testing)
5. [Security Testing](#security-testing)
6. [Final Acceptance Testing](#final-acceptance-testing)
7. [Deployment Verification](#deployment-verification)
8. [References](#references)

---

## Testing Strategy Overview

### Testing Focus for Phase 5

| Component | Test Types | Priority |
|-----------|------------|----------|
| Docker Images | Container tests, security scans | High |
| API Documentation | OpenAPI validation | High |
| Performance | Load tests, stress tests | High |
| Security | Vulnerability scans, OWASP checks | High |
| Integration | Full system tests | High |

### Testing Tools

| Tool | Purpose | Version |
|------|---------|---------|
| Testcontainers | Container integration tests | 1.19+ |
| Trivy | Container security scanning | Latest |
| OWASP ZAP | Security testing | Latest |
| k6 | Load/stress testing | Latest |
| Spectral | OpenAPI linting | Latest |

---

## Container Testing

### Docker Image Tests

Create `tests/docker/test-container.sh`:

```bash
#!/bin/bash
set -e

echo "=== Docker Container Tests ==="

IMAGE_NAME="trading-platform/api:latest"

# Test 1: Image builds successfully
echo "1. Testing image build..."
docker build -t $IMAGE_NAME . || { echo "FAIL: Image build failed"; exit 1; }
echo "   PASS: Image built successfully"

# Test 2: Container starts
echo "2. Testing container startup..."
CONTAINER_ID=$(docker run -d -p 8888:8080 $IMAGE_NAME)
sleep 30

# Test 3: Health check passes
echo "3. Testing health check..."
HEALTH=$(curl -s http://localhost:8888/actuator/health | jq -r '.status')
if [ "$HEALTH" == "UP" ]; then
    echo "   PASS: Health check passed"
else
    echo "   FAIL: Health check failed"
    docker logs $CONTAINER_ID
    docker stop $CONTAINER_ID
    exit 1
fi

# Test 4: API responds
echo "4. Testing API response..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/api/health)
if [ "$STATUS" == "200" ]; then
    echo "   PASS: API responding"
else
    echo "   FAIL: API not responding (status: $STATUS)"
    docker stop $CONTAINER_ID
    exit 1
fi

# Cleanup
docker stop $CONTAINER_ID
docker rm $CONTAINER_ID

echo "=== All Container Tests Passed ==="
```

**PowerShell Version** - Create `tests/docker/test-container.ps1`:

```powershell
Write-Host "=== Docker Container Tests ===" -ForegroundColor Cyan

$IMAGE_NAME = "trading-platform/api:latest"

# Test 1: Image builds successfully
Write-Host "1. Testing image build..."
docker build -t $IMAGE_NAME .
if ($LASTEXITCODE -ne 0) {
    Write-Host "   FAIL: Image build failed" -ForegroundColor Red
    exit 1
}
Write-Host "   PASS: Image built successfully" -ForegroundColor Green

# Test 2: Container starts
Write-Host "2. Testing container startup..."
$CONTAINER_ID = docker run -d -p 8888:8080 $IMAGE_NAME
Start-Sleep -Seconds 30

# Test 3: Health check passes
Write-Host "3. Testing health check..."
try {
    $health = Invoke-RestMethod -Uri http://localhost:8888/actuator/health
    if ($health.status -eq "UP") {
        Write-Host "   PASS: Health check passed" -ForegroundColor Green
    } else {
        throw "Health check failed"
    }
} catch {
    Write-Host "   FAIL: Health check failed" -ForegroundColor Red
    docker logs $CONTAINER_ID
    docker stop $CONTAINER_ID
    exit 1
}

# Test 4: API responds
Write-Host "4. Testing API response..."
try {
    $response = Invoke-RestMethod -Uri http://localhost:8888/api/health
    Write-Host "   PASS: API responding" -ForegroundColor Green
} catch {
    Write-Host "   FAIL: API not responding" -ForegroundColor Red
    docker stop $CONTAINER_ID
    exit 1
}

# Cleanup
docker stop $CONTAINER_ID
docker rm $CONTAINER_ID

Write-Host "=== All Container Tests Passed ===" -ForegroundColor Green
```

### Security Scanning

**Bash (Linux/macOS):**
```bash
# Install Trivy
brew install trivy

# Scan image for vulnerabilities
trivy image trading-platform/api:latest

# Scan with specific severity
trivy image --severity HIGH,CRITICAL trading-platform/api:latest

# Generate report
trivy image --format json -o trivy-report.json trading-platform/api:latest
```

**PowerShell (Windows 10/11):**
```powershell
# Install Trivy
choco install trivy

# Scan image for vulnerabilities
trivy image trading-platform/api:latest

# Scan with specific severity
trivy image --severity HIGH,CRITICAL trading-platform/api:latest

# Generate report
trivy image --format json -o trivy-report.json trading-platform/api:latest
```

---

## API Documentation Testing

### OpenAPI Validation

Create `tests/api/validate-openapi.sh`:

```bash
#!/bin/bash
echo "=== OpenAPI Validation ==="

# Install Spectral
npm install -g @stoplight/spectral-cli

# Download OpenAPI spec
curl -s http://localhost:8080/v3/api-docs > openapi.json

# Validate with Spectral
spectral lint openapi.json --ruleset .spectral.yml

# Check for required endpoints
echo "Checking required endpoints..."
ENDPOINTS=(
    "/api/articles"
    "/api/articles/{id}"
    "/api/prices/historical"
    "/api/prices/current/{symbol}"
    "/api/analysis/{articleId}"
    "/api/health"
)

for endpoint in "${ENDPOINTS[@]}"; do
    if grep -q "\"$endpoint\"" openapi.json; then
        echo "   ✓ $endpoint documented"
    else
        echo "   ✗ $endpoint missing"
    fi
done

echo "=== Validation Complete ==="
```

### Contract Testing

Create `src/test/java/com/example/backend/contract/ApiContractTest.java`:

```java
package com.example.backend.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void articlesEndpointShouldReturnPagedResponse() throws Exception {
        mockMvc.perform(get("/api/articles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }
    
    @Test
    void healthEndpointShouldReturnRequiredFields() throws Exception {
        mockMvc.perform(get("/api/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.service").exists());
    }
    
    @Test
    void priceEndpointShouldReturnCorrectFormat() throws Exception {
        mockMvc.perform(get("/api/prices/historical")
                .param("symbol", "BTCUSDT")
                .param("interval", "1h")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    void openApiDocsShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.0.1"))
                .andExpect(jsonPath("$.info.title").exists())
                .andExpect(jsonPath("$.paths").exists());
    }
}
```

---

## Performance Testing

### Stress Test Script

Create `tests/performance/stress-test.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

export const options = {
  scenarios: {
    // Stress test - push to breaking point
    stress: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 5000,
      stages: [
        { duration: '2m', target: 100 },   // Warm up
        { duration: '5m', target: 500 },   // Ramp to 500 req/s
        { duration: '5m', target: 1000 },  // Ramp to 1000 req/s
        { duration: '5m', target: 2000 },  // Ramp to 2000 req/s
        { duration: '5m', target: 5000 },  // Ramp to 5000 req/s (stress)
        { duration: '2m', target: 0 },     // Recovery
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],   // 95% under 1s
    http_req_failed: ['rate<0.05'],       // Less than 5% failures under stress
    errors: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Randomize endpoint to simulate real traffic
  const endpoints = [
    '/api/health',
    '/api/articles?page=0&size=10',
    '/api/prices/current/BTCUSDT',
    '/api/prices/historical?symbol=BTCUSDT&interval=1h&limit=50',
  ];
  
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const start = Date.now();
  
  const res = http.get(`${BASE_URL}${endpoint}`);
  
  responseTime.add(Date.now() - start);
  
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 1s': (r) => r.timings.duration < 1000,
  });
  
  if (!success) {
    errorRate.add(1);
  }
  
  sleep(0.1);
}
```

### Run Stress Tests

**Bash (Linux/macOS):**
```bash
# Run stress test
k6 run tests/performance/stress-test.js

# Run with custom base URL
k6 run --env BASE_URL=http://localhost:8080 tests/performance/stress-test.js

# Save results
k6 run --out json=stress-results.json tests/performance/stress-test.js
```

**PowerShell (Windows 10/11):**
```powershell
# Run stress test
k6 run tests/performance/stress-test.js

# Run with custom base URL
k6 run --env BASE_URL=http://localhost:8080 tests/performance/stress-test.js

# Save results
k6 run --out json=stress-results.json tests/performance/stress-test.js
```

---

## Security Testing

### OWASP ZAP Scanning

**Bash (Linux/macOS):**
```bash
# Run ZAP baseline scan
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t http://host.docker.internal:8080 \
  -r zap-report.html

# Run API scan
docker run -t owasp/zap2docker-stable zap-api-scan.py \
  -t http://host.docker.internal:8080/v3/api-docs \
  -f openapi \
  -r zap-api-report.html
```

**PowerShell (Windows 10/11):**
```powershell
# Run ZAP baseline scan
docker run -t owasp/zap2docker-stable zap-baseline.py `
  -t http://host.docker.internal:8080 `
  -r zap-report.html

# Run API scan
docker run -t owasp/zap2docker-stable zap-api-scan.py `
  -t http://host.docker.internal:8080/v3/api-docs `
  -f openapi `
  -r zap-api-report.html
```

---

## Final Acceptance Testing

### Manual Testing Checklist

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| MT-501 | Docker images built | All images exist | ☐ |
| MT-502 | Containers start healthy | All health checks pass | ☐ |
| MT-503 | API docs accessible | Swagger UI loads | ☐ |
| MT-504 | All endpoints work | 200 responses | ☐ |
| MT-505 | Load test passes | p95 < 500ms at 5000 users | ☐ |
| MT-506 | Security scan clean | No critical vulnerabilities | ☐ |

### Acceptance Criteria

| Criteria | Test Method | Target | Status |
|----------|-------------|--------|--------|
| Docker images ready | Build test | All services containerized | ☐ |
| API documented | OpenAPI check | 100% endpoint coverage | ☐ |
| Load test: 5000 users | k6 stress test | p95 < 500ms | ☐ |
| Error rate under load | k6 stress test | < 1% | ☐ |
| No critical CVEs | Trivy scan | 0 critical vulnerabilities | ☐ |

### Final Verification Script

**Bash (Linux/macOS):**
```bash
#!/bin/bash
echo "=== Phase 5 Final Verification ==="

PASS=0
FAIL=0

# 1. Docker Images
echo "1. Checking Docker images..."
if docker images | grep -q "trading-platform/api"; then
    echo "   ✓ API image exists"
    ((PASS++))
else
    echo "   ✗ API image missing"
    ((FAIL++))
fi

# 2. Container Health
echo "2. Checking container health..."
HEALTH=$(curl -s http://localhost:8080/actuator/health | jq -r '.status')
if [ "$HEALTH" == "UP" ]; then
    echo "   ✓ Application healthy"
    ((PASS++))
else
    echo "   ✗ Application unhealthy"
    ((FAIL++))
fi

# 3. API Documentation
echo "3. Checking API documentation..."
SWAGGER=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html)
if [ "$SWAGGER" == "200" ]; then
    echo "   ✓ Swagger UI accessible"
    ((PASS++))
else
    echo "   ✗ Swagger UI not accessible"
    ((FAIL++))
fi

# 4. OpenAPI Spec
echo "4. Checking OpenAPI spec..."
OPENAPI=$(curl -s http://localhost:8080/v3/api-docs | jq -r '.openapi')
if [ "$OPENAPI" == "3.0.1" ]; then
    echo "   ✓ OpenAPI spec valid"
    ((PASS++))
else
    echo "   ✗ OpenAPI spec invalid"
    ((FAIL++))
fi

# 5. All Endpoints
echo "5. Checking all endpoints..."
ENDPOINTS=(
    "/api/health"
    "/api/articles"
    "/api/prices/symbols"
)
for endpoint in "${ENDPOINTS[@]}"; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080$endpoint")
    if [ "$STATUS" == "200" ]; then
        echo "   ✓ $endpoint responding"
    else
        echo "   ✗ $endpoint failed (status: $STATUS)"
        ((FAIL++))
    fi
done

# Summary
echo ""
echo "=== Summary ==="
echo "Passed: $PASS"
echo "Failed: $FAIL"

if [ $FAIL -eq 0 ]; then
    echo "✓ All verification checks passed!"
    exit 0
else
    echo "✗ Some verification checks failed"
    exit 1
fi
```

**PowerShell (Windows 10/11):**
```powershell
Write-Host "=== Phase 5 Final Verification ===" -ForegroundColor Cyan

$pass = 0
$fail = 0

# 1. Docker Images
Write-Host "1. Checking Docker images..."
$images = docker images | Select-String "trading-platform/api"
if ($images) {
    Write-Host "   ✓ API image exists" -ForegroundColor Green
    $pass++
} else {
    Write-Host "   ✗ API image missing" -ForegroundColor Red
    $fail++
}

# 2. Container Health
Write-Host "2. Checking container health..."
try {
    $health = Invoke-RestMethod -Uri http://localhost:8080/actuator/health -ErrorAction Stop
    if ($health.status -eq "UP") {
        Write-Host "   ✓ Application healthy" -ForegroundColor Green
        $pass++
    }
} catch {
    Write-Host "   ✗ Application unhealthy" -ForegroundColor Red
    $fail++
}

# 3. API Documentation
Write-Host "3. Checking API documentation..."
try {
    $swagger = Invoke-WebRequest -Uri http://localhost:8080/swagger-ui/index.html -UseBasicParsing
    if ($swagger.StatusCode -eq 200) {
        Write-Host "   ✓ Swagger UI accessible" -ForegroundColor Green
        $pass++
    }
} catch {
    Write-Host "   ✗ Swagger UI not accessible" -ForegroundColor Red
    $fail++
}

# 4. OpenAPI Spec
Write-Host "4. Checking OpenAPI spec..."
try {
    $openapi = Invoke-RestMethod -Uri http://localhost:8080/v3/api-docs -ErrorAction Stop
    if ($openapi.openapi -eq "3.0.1") {
        Write-Host "   ✓ OpenAPI spec valid" -ForegroundColor Green
        $pass++
    }
} catch {
    Write-Host "   ✗ OpenAPI spec invalid" -ForegroundColor Red
    $fail++
}

# Summary
Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host "Passed: $pass" -ForegroundColor Green
Write-Host "Failed: $fail" -ForegroundColor $(if($fail -gt 0){"Red"}else{"Green"})

if ($fail -eq 0) {
    Write-Host "✓ All verification checks passed!" -ForegroundColor Green
} else {
    Write-Host "✗ Some verification checks failed" -ForegroundColor Red
}
```

---

## References

### Testing Documentation

- [Trivy Security Scanner](https://aquasecurity.github.io/trivy/)
- [OWASP ZAP](https://www.zaproxy.org/docs/)
- [Spectral API Linter](https://stoplight.io/open-source/spectral)
- [k6 Documentation](https://k6.io/docs/)

### Related Project Documents

- [Phase5-ImplementationGuide.md](./Phase5-ImplementationGuide.md) - Implementation guide
- [Architecture.md](../Architecture.md) - System architecture
- [Operations.md](../Operations.md) - CI/CD and deployment
