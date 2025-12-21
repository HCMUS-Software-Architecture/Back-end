# TRADING PLATFORM - API SPECIFICATION

All routes except login, register and refresh need to add 'Authorization: Bearer' to header

## Health Check Service

### GET `/api/health/health`
Check system health status

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2024-01-01T00:00:00",
  "service": "trading-platform-api"
}
```

### GET `/api/health/version`
Get system version information

**Response:**
```json
{
  "version": "1.0.0-SNAPSHOT",
  "phase": "1"
}
```

## Authentication Service

### POST `/api/auth/register`
Register a new user

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "userName": "John Doe"
}
```

**Response (201):**
```json
{
  "message": "User has been registered successfully"
}
```

**Error Response (400):**
```json
{
  "error": "Bad Request",
  "message": "Email already exists"
}
```

### POST `/api/auth/login`
Authenticate user and get tokens

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401):**
```json
{
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

### POST `/api/auth/refresh`
Refresh access token using refresh token

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401):**
```json
{
  "error": "Unauthorized",
  "message": "Invalid refresh token"
}
```

### GET `/api/auth/logout`
Logout user by revoking refresh token


**Response (200):**
```json
{
  "message": "User has been logged out"
}
```

### GET `/api/me`
Get current authenticated user information

**Headers:**
- `Authorization: Bearer <accessToken>`

**Response (200):**
```json
{
  "id": "uuid",
  "fullName": "John Doe"
}
```

**Error Response (401):**
```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing token"
}
```

**Error Response (404):**
```json
{
  "error": "Not Found",
  "message": "User not found"
}
```

## Article Service

### GET `/api/articles`
Get all articles with pagination

**Query Parameters:**
- `page` (int, optional): Page number, default = 0
- `size` (int, optional): Number of articles per page, default = 20
- `sortBy` (string, optional): Sort field, default = "publishedAt"
- `direction` (string, optional): Sort direction (asc/desc), default = "desc"

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Article title",
      "content": "Article content",
      "source": "Source name",
      "url": "https://...",
      "publishedAt": "2024-01-01T00:00:00",
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5
}
```

### GET `/api/articles/{id}`
Get article details

**Path Parameters:**
- `id` (String): Article UUID

**Response:**
```json
{
  "id": "uuid",
  "title": "Article title",
  "content": "Article content",
  "source": "Source name",
  "url": "https://...",
  "publishedAt": "2024-01-01T00:00:00",
  "createdAt": "2024-01-01T00:00:00"
}
```

**Error Response (404):**
```json
{
  "error": "Not Found"
}
```

### GET `/api/articles/source/{source}`
Get articles by source

**Path Parameters:**
- `source` (String): Source name

**Query Parameters:**
- `page` (int, optional): Page number, default = 0
- `size` (int, optional): Number of articles per page, default = 20

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Article title",
      "content": "Article content",
      "source": "Source name",
      "url": "https://...",
      "publishedAt": "2024-01-01T00:00:00",
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 50,
  "totalPages": 3
}
```

### GET `/api/articles/search`
Search articles

**Query Parameters:**
- `q` (String, required): Search keyword
- `page` (int, optional): Page number, default = 0
- `size` (int, optional): Number of articles per page, default = 20

**Example:**
```
GET /api/articles/search?q=bitcoin&page=0&size=20
```

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Article about Bitcoin",
      "content": "Article content...",
      "source": "Source name",
      "url": "https://...",
      "publishedAt": "2024-01-01T00:00:00",
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 25,
  "totalPages": 2
}
```

## Price Service

### GET `/api/prices/current/{symbol}`
Get current price of a trading symbol

**Path Parameters:**
- `symbol` (String): Trading symbol (e.g., BTCUSDT, ETHUSDT)

**Example:**
```
GET /api/prices/current/BTCUSDT
```

**Response:**
```json
{
  "symbol": "BTCUSDT",
  "price": 45000.50,
  "timestamp": 1704067200000
}
```

**Error Response (404):**
```json
{
  "error": "Not Found"
}
```

### GET `/api/prices/historical`
Get historical candlestick data

**Query Parameters:**
- `symbol` (String, optional): Trading symbol, default = "BTCUSDT"
- `interval` (String, optional): Time interval (1m, 5m, 15m, 1h, 4h, 1d), default = "1h"
- `limit` (int, optional): Number of candles, default = 100

**Example:**
```
GET /api/prices/historical?symbol=BTCUSDT&interval=1h&limit=50
```

**Response:**
```json
[
  {
    "id": 1,
    "symbol": "BTCUSDT",
    "interval": "1h",
    "openTime": 1704067200000,
    "closeTime": 1704070800000,
    "open": 45000.00,
    "high": 45500.00,
    "low": 44800.00,
    "close": 45200.00,
    "volume": 1250.5,
    "quoteVolume": 56500000.0
  }
]
```

### GET `/api/prices/symbols`
Get list of available trading symbols

**Response:**
```json
[
  "BTCUSDT",
  "ETHUSDT",
  "BNBUSDT"
]
```

## Error Responses

All endpoints may return the following error codes:

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "Invalid input data"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```