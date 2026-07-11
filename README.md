# OTA Hotel Booking Integration

Fault-tolerant microservices platform that ingests hotel inventory and rates from fragmented
third-party OTA providers, normalizes them into a unified model, persists them with ACID
guarantees in MySQL, and exposes booking + availability via REST APIs.

## Architecture

```
                                  ┌─────────────────────┐
                                  │   API Gateway :8080  │
                                  │ Spring Cloud Gateway │
                                  └──────────┬──────────┘
                      ┌───────────────────────┼───────────────────────┐
                      │                       │                       │
              ┌───────▼──────┐       ┌─────────▼─────────┐     ┌───────▼──────────────┐
              │ hotel-service│       │ booking-service   │     │ ota-integration-     │
              │   :8082      │       │    :8083          │     │ service :8081        │
              │ Inventory +  │       │ ACID bookings +   │     │ Provider adapters +  │
              │ availability │       │ idempotency +     │     │ fault-tolerant       │
              │ (MySQL)      │◄──────│ Feign→hotel       │◄────│ pipeline + DLQ       │
              └──────────────┘       └───────────────────┘     └──────────┬───────────┘
                      ▲                                                    │
                      └────────────── Feign upsert ───────────────────────┘
```

## Modules

| Module                    | Port | Responsibility                                                     |
|---------------------------|------|-------------------------------------------------------------------|
| `common`                  | -    | Shared API DTOs, error envelope, global exception handler        |
| `hotel-service`           | 8082 | Hotel inventory, room types, daily rates, availability, reserve  |
| `booking-service`         | 8083 | Bookings (ACID), idempotency, saga-style confirm/fail over hotel |
| `ota-integration-service` | 8081 | Provider adapters, normalize, Feign→hotel, retry+circuit+DLQ     |
| `api-gateway`             | 8080 | Spring Cloud Gateway routing                                      |

## Tech Stack

- Java 21, Maven (multi-module reactor)
- Spring Boot 3.5.16, Spring Cloud 2025.0.3
- Spring Data JPA, Flyway, MySQL 8 / H2 (local)
- Spring Cloud OpenFeign (inter-service calls)
- Spring Retry + Resilience4j circuit breaker (fault tolerance)
- Spring Cloud Gateway

## Quick Start (local, no Docker)

Each service runs on its own port. By default profiles use MySQL; for a quick local run use the
`h2` profile (in-memory, schema created by Flyway in MySQL mode).

```bash
# terminal 1
cd hotel-service   && mvn spring-boot:run -Dspring-boot.run.profiles=h2
# terminal 2
cd booking-service && mvn spring-boot:run -Dspring-boot.run.profiles=h2
# terminal 3
cd ota-integration-service && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Full Stack with Docker

```bash
docker compose up --build
```

This starts MySQL + all four services + the gateway. The gateway is on `:8080`.

## API Usage

### 1. Trigger a sync from the OTA providers

```bash
curl -X POST http://localhost:8081/api/v1/sync
```

Returns a `SyncRunResult` summarizing fetched/normalized/upserted counts per provider.

### 2. Search hotels

```bash
curl "http://localhost:8082/api/v1/hotels?city=Paris"
```

### 3. Check availability

```bash
curl "http://localhost:8082/api/v1/hotels/{hotelId}/availability?checkIn=2026-08-01&checkOut=2026-08-04"
```

### 4. Create a booking (idempotent)

```bash
curl -X POST http://localhost:8083/api/v1/bookings \
  -H 'Content-Type: application/json' \
  -d '{"hotelId":1,"roomTypeId":1,"checkIn":"2026-08-01","checkOut":"2026-08-04","guestName":"Alice","guests":2,"idempotencyKey":"k-001"}'
```

Replaying the same `idempotencyKey` returns the original booking (no double charge).

### 5. Dead-letter & replay

```bash
curl http://localhost:8081/api/v1/sync/dead-letter
curl -X POST http://localhost:8081/api/v1/sync/dead-letter/replay
```

## Fault Tolerance

- **Retries**: every provider fetch and every push to the hotel service is retried with backoff
  (Spring Retry).
- **Circuit breaker**: each provider is wrapped in a Resilience4j circuit breaker so a sick
  provider fails fast instead of stacking threads.
- **Dead-letter**: any provider batch that exhausts retries is parked in a `dead_letter` table and
  retried by a scheduled replayer (or manually via the replay endpoint).

## Testing

```bash
mvn clean install
```

Each service has a `@SpringBootTest` integration test exercising the happy path and the failure
path (H2 profile, Feign clients mocked).

## Sample Data

Provider fixtures ship in `ota-integration-service/src/main/resources/fixtures/`:
`acme-inventory.json` and `globex-inventory.json`. They model two *different* vendor JSON shapes
(fragmented sources) that the adapters normalize into one unified schema.

To simulate a provider outage, set the env vars `OTA_ACME_FAIL=true` / `OTA_GLOBEX_FAIL=true`.

## Databases

| Service                  | Database        |
|--------------------------|-----------------|
| `hotel-service`          | `ota_hotels`    |
| `booking-service`        | `ota_bookings`  |
| `ota-integration-service`| `ota_integration`|

Schema is managed by Flyway (`db/migration/V1__init_schema.sql` in each service).
