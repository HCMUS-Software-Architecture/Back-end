# Microservices Migration Status

## ✅ Migration Complete

The monolith has been successfully migrated to the following microservices:

| Service              | Port | Description                                 |
| -------------------- | ---- | ------------------------------------------- |
| **api-gateway**      | 8081 | Gateway, CORS, JWT validation, routing      |
| **user-service**     | 8082 | Auth, User management (MongoDB)             |
| **price-service**    | 8083 | Price data, WebSocket, Candles (PostgreSQL) |
| **discovery-server** | 8761 | Eureka service registry                     |

---

## 📁 Environment Files

### ✅ Use: `.env`

The Docker Compose file uses `.env` for all microservices:

```yaml
env_file:
  - .env
```

### ❌ Delete: `micro.env`

`micro.env` is **UNUSED** and can be safely deleted:

- Not referenced in `docker-compose.yml`
- Only mentioned in old documentation
- `.env` contains all the same variables

**Action:** Delete `micro.env` and `micro.env.example`

---

## 🗑️ Files Safe to Delete in `src/`

### Controllers (Migrated)

- ✅ `src/main/java/com/example/backend/controller/AuthController.java` → user-service
- ✅ `src/main/java/com/example/backend/controller/UserController.java` → user-service
- ✅ `src/main/java/com/example/backend/controller/PriceController.java` → price-service
- ✅ `src/main/java/com/example/backend/controller/HealthController.java` → api-gateway

### Services (Migrated)

- ✅ `src/main/java/com/example/backend/service/AuthService.java` → user-service
- ✅ `src/main/java/com/example/backend/service/UserService.java` → user-service
- ✅ `src/main/java/com/example/backend/service/JwtService.java` → user-service
- ✅ `src/main/java/com/example/backend/service/candle/*` → price-service
- ✅ `src/main/java/com/example/backend/service/collector/*` → price-service

### Config (Migrated)

- ✅ `src/main/java/com/example/backend/config/CorsConfig.java` → api-gateway
- ✅ `src/main/java/com/example/backend/config/SecurityConfig.java` → each microservice
- ✅ `src/main/java/com/example/backend/config/WebSocketConfig.java` → price-service

### Filters (Migrated)

- ✅ `src/main/java/com/example/backend/filter/JwtAuthFilter.java` → api-gateway

### DTOs (Duplicated to microservices)

- ✅ All DTOs in `src/main/java/com/example/backend/dto/` are copied to respective services

### Entities/Models (Duplicated)

- ✅ Price entities → price-service
- ✅ User/Auth models → user-service

### Repositories (Duplicated)

- ✅ Price repositories → price-service
- ✅ User/Auth repositories → user-service

---

## ⏸️ Keep for Future: Article Service

The following files should be **KEPT** for future article-service migration:

- `src/main/java/com/example/backend/controller/ArticleController.java`
- `src/main/java/com/example/backend/service/ArticleService.java`
- `src/main/java/com/example/backend/service/CrawlerService.java`
- `src/main/java/com/example/backend/model/Article.java`
- `src/main/java/com/example/backend/repository/ArticleRepository.java`
- `src/main/java/com/example/backend/repository/mongodb/ArticleDocumentRepository.java`

---

## 🔧 Recommended Cleanup Commands

```powershell
# Delete unused env file
Remove-Item micro.env
Remove-Item micro.env.example

# After validating all services work, you can delete the entire monolith src/
# (Keep article-related files if article-service isn't ready yet)
```

---

## 🏃 Running the Microservices

```powershell
# Start all services
docker compose up -d

# Rebuild after changes
docker compose up --build -d

# View logs
docker compose logs -f api-gateway user-service price-service
```

---

## 🧪 Verification Endpoints

| Endpoint                                             | Expected             |
| ---------------------------------------------------- | -------------------- |
| `GET http://localhost:8081/api/health`               | 200 OK               |
| `POST http://localhost:8081/api/auth/register`       | 201 Created          |
| `POST http://localhost:8081/api/auth/login`          | 200 + tokens         |
| `GET http://localhost:8081/api/user/me` (with token) | 200 + user data      |
| `GET http://localhost:8081/api/prices/historical`    | 200 + candles        |
| `WS http://localhost:8081/ws/prices`                 | WebSocket connection |

---

_Last updated: January 24, 2026_
