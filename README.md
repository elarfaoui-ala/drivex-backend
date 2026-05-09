# DriveX Backend — Spring Boot

Full delivery driver backend with JWT auth, H2 in-memory DB, Redis cache, WebSocket real-time events.

---

## Tech stack

| Layer       | Technology                        |
|-------------|-----------------------------------|
| Framework   | Spring Boot 3.2 + Java 21         |
| Database    | **H2** (dev) / PostgreSQL (prod)  |
| Cache       | **Redis** (Lettuce client)        |
| Auth        | JWT (JJWT 0.12) + Spring Security |
| Real-time   | WebSocket STOMP over SockJS       |
| API docs    | Springdoc OpenAPI (Swagger UI)    |
| Build       | Maven                             |

---

## Quick start (5 minutes)

### 1 — Start Redis
```bash
docker compose up -d
```
Redis runs on `localhost:6379`.  
Redis Commander UI: http://localhost:8081

> **No Redis?** The app gracefully falls back to in-memory cache.
> Just run the app — it will warn but continue working.

### 2 — Run the app
```bash
mvn spring-boot:run
```

### 3 — Open the tools

| Tool               | URL                                     |
|--------------------|-----------------------------------------|
| Swagger UI         | http://localhost:8080/swagger-ui.html   |
| H2 Console         | http://localhost:8080/h2-console        |
| Redis Commander    | http://localhost:8081                   |

### 4 — Log in via Swagger

1. Go to **Swagger UI**
2. Click `POST /api/v1/auth/login`
3. Use: `alex@drivex.com` / `password123`
4. Copy `accessToken` from the response
5. Click **Authorize** (top right) → paste token
6. All protected endpoints now work

---

## Test accounts (seeded on startup)

| Name        | Email               | Password     | Driver ID  |
|-------------|---------------------|--------------|------------|
| Alex Kumar  | alex@drivex.com     | password123  | drv-0001   |
| Sara Amara  | sara@drivex.com     | password123  | drv-0002   |
| James Okafor| james@drivex.com    | password123  | drv-0003   |

---

## H2 Console

URL: http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:drivexdb`  
Username: `sa`  Password: *(empty)*

The schema is auto-created and seeded with 3 drivers + 5 orders on startup.

---

## API endpoints

### Auth
```
POST /api/v1/auth/register      Register new driver
POST /api/v1/auth/login         Login → returns JWT
POST /api/v1/auth/refresh       Refresh access token
```

### Drivers
```
GET    /api/v1/drivers               List all drivers
GET    /api/v1/drivers/online        Online drivers only
GET    /api/v1/drivers/{id}          Get profile (cached in Redis 5 min)
PATCH  /api/v1/drivers/{id}/status   Update status: ONLINE | OFFLINE | BREAK
POST   /api/v1/drivers/{id}/location REST location ping (fallback)
GET    /api/v1/drivers/{id}/location Last known position (from Redis)
```

### Orders
```
GET    /api/v1/orders/available              Available orders (no auth needed)
GET    /api/v1/orders/active                 In-progress orders
GET    /api/v1/orders/{id}                   Full order detail
GET    /api/v1/orders/driver/{id}?page&size  Driver's paginated history
POST   /api/v1/orders/{id}/accept            Accept order
PATCH  /api/v1/orders/{id}/status            Advance status
DELETE /api/v1/orders/{id}                   Cancel order
```

Order status lifecycle:
```
NEW → ACCEPTED → PICKED_UP → EN_ROUTE → DELIVERED
            ↘               ↘            ↘
           CANCELLED      CANCELLED    CANCELLED
```

### Earnings
```
GET /api/v1/earnings/{id}/today          Today's summary (cached 2 min)
GET /api/v1/earnings/{id}/week           Current week (cached 5 min)
GET /api/v1/earnings/{id}/month          Current month (cached 10 min)
GET /api/v1/earnings/{id}/custom?from&to Custom range
```

---

## WebSocket

Connect to: `ws://localhost:8080/ws` (SockJS)

**STOMP CONNECT header:**
```
Authorization: Bearer <accessToken>
```

**Subscribe to:**
```
/topic/orders/new                     → new available orders
/topic/orders/{id}/status             → order status changes
/topic/drivers/{id}/location          → driver GPS updates
/user/queue/events                    → personal driver events
```

**Send location ping:**
```
/app/location
{
  "driverId": "drv-0001",
  "lat": 40.7550,
  "lng": -73.9890,
  "heading": 180.0,
  "speedKmh": 45.0,
  "timestamp": "2024-04-22T14:30:00"
}
```

---

## Redis cache keys

| Key pattern               | TTL     | Content                        |
|---------------------------|---------|--------------------------------|
| `driver:profile:<id>`     | 5 min   | DriverSummary JSON             |
| `drivers:online`          | 30 sec  | List of online DriverSummary   |
| `orders:available`        | 1 min   | List of NEW OrderSummary       |
| `order:detail:<id>`       | 2 min   | Full OrderDetail JSON          |
| `driver:location:<id>`    | 10 min  | LocationPayload JSON           |
| `earnings:today:<id>`     | 2 min   | EarningsSummary JSON           |
| `earnings:week:<id>`      | 5 min   | EarningsSummary JSON           |
| `earnings:month:<id>`     | 10 min  | EarningsSummary JSON           |

---

## Running tests

```bash
# All tests (Redis disabled for tests — uses in-memory cache)
mvn test

# Single class
mvn test -Dtest=OrderServiceTest
mvn test -Dtest=JwtServiceTest
mvn test -Dtest=DrivexAuthIntegrationTest
```

---

## Switch to PostgreSQL (production)

1. Start PostgreSQL:
```bash
# Uncomment the postgres service in docker-compose.yml, then:
docker compose up -d postgres
```

2. Run with prod profile:
```bash
mvn spring-boot:run -Dspring.profiles.active=prod \
  -DDBHOST=localhost -DDB_NAME=drivexdb \
  -DDB_USER=drivex -DDB_PASSWORD=drivex_dev_password
```

The only code change needed is in `application.yml` — the prod profile block is already there.

---

## Project structure

```
src/main/java/com/drivex/
├── DrivexApplication.java          Entry point
├── config/
│   ├── DataInitializer.java        Seeds H2 with test data
│   ├── JwtProperties.java          JWT config binding
│   ├── OpenApiConfig.java          Swagger / OpenAPI setup
│   ├── RedisConfig.java            Redis template + cache manager
│   ├── SecurityConfig.java         JWT filter chain, CORS, public routes
│   └── WebSocketConfig.java        STOMP broker config
├── controller/
│   ├── AuthController.java         POST /auth/**
│   ├── DriverController.java       GET|PATCH /drivers/**
│   ├── EarningsController.java     GET /earnings/**
│   └── OrderController.java        GET|POST|PATCH /orders/**
├── dto/
│   └── Dtos.java                   All Java records (request + response)
├── entity/
│   ├── Driver.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Vehicle.java
├── exception/
│   └── ApiException.java           + GlobalExceptionHandler
├── repository/
│   ├── DriverRepository.java
│   └── OrderRepository.java
├── security/
│   ├── DriverUserDetailsService.java
│   ├── JwtAuthFilter.java
│   └── JwtService.java
├── service/
│   ├── AuthService.java
│   ├── DriverService.java
│   ├── EarningService.java
│   ├── LocationService.java        + LocationWsHandler (STOMP)
│   └── OrderService.java
└── websocket/
    ├── JwtChannelInterceptor.java
    ├── LocationBroadcaster.java
    └── OrderEventPublisher.java
```
