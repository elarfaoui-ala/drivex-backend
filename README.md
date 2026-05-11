# DriveX Backend

Backend API for the DriveX delivery driver mobile app.  
Built with Spring Boot 3.4 + Java 24, deployed on Railway.

**Production:** https://api-drivex.up.railway.app  
**Swagger UI:** https://api-drivex.up.railway.app/swagger-ui.html

---

## Tech Stack

| Layer       | Technology                              |
|-------------|-----------------------------------------|
| Framework   | Spring Boot 3.4 + Java 24              |
| Database    | H2 (dev) / PostgreSQL (prod)           |
| Cache       | Redis via Upstash (Lettuce client)     |
| Auth        | JWT (JJWT 0.12) + Spring Security      |
| Real-time   | WebSocket STOMP over SockJS            |
| API docs    | Springdoc OpenAPI (Swagger UI)         |
| Email       | JavaMailSender (SMTP — Mailgun/SendGrid) |
| Build       | Maven                                  |
| Deploy      | Docker → Railway                       |

---

## Quick Start (Local Dev)

### Prerequisites
- Java 24
- Docker Desktop (optional — for Redis)

### 1. Start Redis (optional)
```bash
docker run -d -p 6379:6379 redis:7-alpine
```
The app falls back to in-memory cache if Redis is unavailable.

### 2. Run
```bash
./mvnw spring-boot:run
```
App starts at http://localhost:8080

### 3. Open Swagger UI
http://localhost:8080/swagger-ui.html

### 4. Login
Use `POST /api/v1/auth/login` with:
```json
{ "email": "alex@drivex.com", "password": "password123" }
```
Copy the `accessToken` → click **Authorize** → paste as `Bearer <token>`.

---

## Test Accounts (Seeded on Startup)

| Name         | Email               | Password     | Driver ID |
|--------------|---------------------|--------------|-----------|
| Alex Kumar   | alex@drivex.com     | password123  | drv-0001  |
| Sara Amara   | sara@drivex.com     | password123  | drv-0002  |
| James Okafor | james@drivex.com    | password123  | drv-0003  |

---

## API Endpoints

### Auth — `/api/v1/auth`
| Method | Path              | Auth | Description                    |
|--------|-------------------|------|--------------------------------|
| POST   | `/register`       | No   | Register new driver + vehicle  |
| POST   | `/login`          | No   | Login → JWT tokens             |
| POST   | `/refresh`        | No   | Refresh access token           |
| POST   | `/forgot-password`| No   | Sends reset link via email     |
| POST   | `/reset-password` | No   | Reset password with token      |

### Drivers — `/api/v1/drivers`
| Method | Path                      | Auth | Description                          |
|--------|---------------------------|------|--------------------------------------|
| GET    | `/`                       | Yes  | List all drivers                     |
| GET    | `/online`                 | Yes  | List online drivers                  |
| GET    | `/{id}`                   | Yes  | Get driver profile                   |
| PUT    | `/{id}/profile`           | Yes  | Update name, email, phone            |
| PATCH  | `/{id}/password`          | Yes  | Change password                      |
| PATCH  | `/{id}/status`            | Yes  | Update status (ONLINE/OFFLINE/BREAK) |
| POST   | `/{id}/location`          | Yes  | REST location ping (fallback)        |
| GET    | `/{id}/location`          | Yes  | Last known location                  |

### Vehicle — `/api/v1/drivers/{id}/vehicle`
| Method | Path | Auth | Description                    |
|--------|------|------|--------------------------------|
| GET    | `/`  | Yes  | Get registered vehicle         |
| PUT    | `/`  | Yes  | Register or update vehicle     |

### Documents — `/api/v1/drivers/{id}/documents`
| Method | Path         | Auth | Description                    |
|--------|--------------|------|--------------------------------|
| GET    | `/`          | Yes  | List all documents             |
| POST   | `/`          | Yes  | Upload a document              |
| DELETE | `/{docId}`   | Yes  | Delete a document              |

### Orders — `/api/v1/orders`
| Method | Path                        | Auth | Description                    |
|--------|-----------------------------|------|--------------------------------|
| GET    | `/available`                | No   | Available (NEW) orders         |
| GET    | `/active`                   | Yes  | In-progress orders             |
| GET    | `/{id}`                     | Yes  | Full order detail              |
| GET    | `/driver/{id}?page&size`    | Yes  | Driver's order history         |
| POST   | `/{id}/accept`              | Yes  | Accept/claim an order          |
| PATCH  | `/{id}/status`              | Yes  | Advance order status           |
| DELETE | `/{id}`                     | Yes  | Cancel order                   |

Order lifecycle:
```
NEW → ACCEPTED → PICKED_UP → EN_ROUTE → DELIVERED
            ↘               ↘            ↘
           CANCELLED       CANCELLED    CANCELLED
```

### Earnings — `/api/v1/earnings/{id}`
| Method | Path            | Auth | Description                    |
|--------|-----------------|------|--------------------------------|
| GET    | `/today`        | Yes  | Today's summary                |
| GET    | `/week`         | Yes  | Current week summary           |
| GET    | `/month`        | Yes  | Current month summary          |
| GET    | `/custom`       | Yes  | Custom date range              |

---

## WebSocket

Connect to: `wss://api-drivex.up.railway.app/ws` (SockJS)

**STOMP CONNECT header:**
```
Authorization: Bearer <accessToken>
```

**Subscribe:**
```
/topic/orders/new              → new available orders
/topic/orders/{id}/status      → order status changes
/topic/drivers/{id}/location   → driver GPS updates
/user/queue/events             → personal events
```

**Send location:**
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

## Environment Variables (Railway)

### Required
| Variable                  | Description                           |
|---------------------------|---------------------------------------|
| `SPRING_PROFILES_ACTIVE`  | `prod`                                |
| `JWT_SECRET`              | Strong random secret                  |
| `REDIS_HOST`              | Upstash Redis endpoint                |
| `REDIS_PASSWORD`          | Upstash Redis token                   |
| `SMTP_HOST`               | SMTP server (e.g. `smtp.mailgun.org`) |
| `SMTP_USERNAME`           | SMTP login                            |
| `SMTP_PASSWORD`           | SMTP password                         |
| `SMTP_FROM`               | Sender address (e.g. `noreply@drivex.com`) |

### Optional
| Variable                        | Default                        | Description                  |
|---------------------------------|--------------------------------|------------------------------|
| `REDIS_PORT`                    | `6380`                         | Upstash SSL port             |
| `REDIS_SSL`                     | `true`                         | Upstash requires SSL         |
| `APP_FRONTEND_RESET_URL`        | frontend reset-password URL    | Used in password reset email |
| `SMTP_PORT`                     | `587`                          | SMTP port                    |
| `SMTP_AUTH`                     | `true`                         | SMTP auth                    |
| `SMTP_TLS`                      | `true`                         | STARTTLS                     |

Railway's PostgreSQL plugin auto-sets `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.

---

## Redis Cache Keys

| Key pattern               | TTL     | Content                      |
|---------------------------|---------|------------------------------|
| `driver:profile:<id>`     | 5 min   | DriverSummary JSON           |
| `drivers:online`          | 30 sec  | List of online drivers       |
| `orders:available`        | 1 min   | List of NEW orders           |
| `order:detail:<id>`       | 2 min   | Full OrderDetail JSON        |
| `driver:location:<id>`    | 10 min  | LocationPayload JSON         |
| `earnings:today:<id>`     | 2 min   | EarningsSummary JSON         |
| `earnings:week:<id>`      | 5 min   | EarningsSummary JSON         |
| `earnings:month:<id>`     | 10 min  | EarningsSummary JSON         |

---

## Project Structure

```
src/main/java/com/drivex/
├── DrivexApplication.java
├── config/
│   ├── DataInitializer.java       Seeds test data
│   ├── OpenApiConfig.java         Swagger / OpenAPI
│   ├── RedisConfig.java           Redis template + cache manager
│   ├── SecurityConfig.java        JWT filter chain, CORS
│   └── WebSocketConfig.java       STOMP broker
├── controller/
│   ├── AuthController.java
│   ├── DocumentController.java
│   ├── DriverController.java
│   ├── EarningsController.java
│   └── OrderController.java
├── dto/
│   └── Dtos.java                  All request/response records
├── entity/
│   ├── Driver.java
│   ├── DriverDocument.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Vehicle.java
├── exception/
│   └── ApiException.java          + GlobalExceptionHandler
├── repository/
│   ├── DriverDocumentRepository.java
│   ├── DriverRepository.java
│   └── OrderRepository.java
├── security/
│   ├── DriverUserDetailsService.java
│   ├── JwtAuthFilter.java
│   └── JwtService.java
├── service/
│   ├── AuthService.java
│   ├── DriverDocumentService.java
│   ├── DriverService.java
│   ├── EarningService.java
│   ├── EmailService.java
│   ├── LocationService.java
│   └── OrderService.java
└── websocket/
    ├── JwtChannelInterceptor.java
    ├── LocationBroadcaster.java
    └── OrderEventPublisher.java
```

---

## Running Tests

```bash
mvn test
```

---

## Deployment

Push to `main` → Railway auto-deploys from Dockerfile.
