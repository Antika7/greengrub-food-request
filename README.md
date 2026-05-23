# Food Request Service

A **REST + gRPC** microservice for managing food requests in the GreenGrub platform. Persists food rows in **PostgreSQL** (Cloud SQL on k8s, Docker locally) with the schema versioned by **Flyway**, and hydrates attached images on read paths via a gRPC call to **image-service**. The service sits behind the API Gateway, which validates JWTs and forwards `X-User-*` identity headers.

---

## Project Structure

```
src/main/java/com/greengrub/food_request/
├── client/
│   └── ImageServiceClient.java        # gRPC client for image-service (upload + hydrate)
├── config/
│   └── CorsConfig.java                # Profile-driven CORS for the React frontend
├── controller/
│   └── FoodRequestController.java     # REST endpoints under /api/v1/food-requests
├── dto/
│   ├── CreateFoodRequestDTO.java      # POST body (validated)
│   ├── FoodRequestDTO.java            # Read response (with hydrated images)
│   ├── ImageDTO.java
│   ├── QuantityDTO.java
│   └── UpdateFoodStatusDTO.java
├── entity/
│   ├── FoodRequest.java               # JPA entity (UUID id, embedded Quantity, imageIds)
│   ├── FoodStatus.java                # PENDING, APPROVED, DONATED, EXPIRED
│   ├── Quantity.java                  # @Embeddable {amount, unit}
│   └── Unit.java                      # KG, SERVINGS
├── exception/
│   ├── ErrorResponse.java             # {timestamp, status, error, message, path}
│   ├── FoodNotFoundException.java
│   ├── GlobalExceptionHandler.java    # @RestControllerAdvice (REST side)
│   ├── ImageHydrationFailedException.java
│   └── ImageUploadFailedException.java
├── grpc/
│   └── FoodGrpcService.java           # @GrpcService — proto FoodService impl
├── interceptor/
│   └── GrpcExceptionInterceptor.java  # Maps app exceptions → gRPC Status (gRPC side)
├── mapper/
│   └── FoodMapper.java                # Entity/DTO ↔ proto, ISO datetime helpers
├── repository/
│   └── FoodRequestRepository.java     # JpaRepository<FoodRequest, String>
├── security/
│   └── GatewayHeadersFilter.java      # X-User-* headers → request attributes
├── service/
│   ├── FoodRequestService.java        # interface
│   └── impl/FoodRequestServiceImpl.java
└── FoodRequestApplication.java

src/main/resources/
├── application.properties             # Shared base (name, port, profile, actuator)
├── application-local.properties       # Postgres docker, gRPC :9091, dev R4J thresholds
├── application-k8s.yml                # Cloud SQL via env, prod R4J thresholds
└── db/migration/
    └── V1__init_food_requests.sql     # Schema (food_requests + image-id join table)
```

---

## Architecture Overview

```
React UI ─────► API Gateway ─────► REST /api/v1/food-requests
                  (validates JWT,                │
                   sets X-User-*)                ▼
                                  ┌─────────────────────────────────┐
                                  │ GatewayHeadersFilter            │ ← X-User-Id → request attribute
                                  ├─────────────────────────────────┤
                                  │ FoodRequestController           │
                                  ├─────────────────────────────────┤
                                  │ FoodRequestServiceImpl          │ ← @Retry + @CircuitBreaker
                                  ├──────────────┬──────────────────┤
                                  │ Postgres     │ ImageServiceClient│ ← gRPC + R4J wrappers
                                  │ (JPA)        │ (image-service)   │
                                  └──────────────┴──────────────────┘
                                          ▲
                                          │
donation-service ─── gRPC ──► FoodGrpcService (port 9091)
                              CreateFood / GetFood / GetFoodsByUser
                              GetFoodsByIds / UpdateFoodStatus
```

Two inbound channels share the same service layer:

- **REST** — the React frontend (via gateway) calls `/api/v1/food-requests/...`
- **gRPC** — donation-service calls `FoodService.GetFoodsByIds` to hydrate the food references it stores as `List<String>` UUIDs (single round trip + single SQL `WHERE id IN (?, …)`)

---

## Things to Know Before Contributing

1. **Two profiles only — `local` and `k8s`.** No `test` profile. Local uses Postgres docker via `application-local.properties`; k8s uses Cloud SQL via `application-k8s.yml` with env vars (`DB_HOST`, `IMAGE_SERVICE_HOST`, `CORS_ALLOWED_ORIGINS`, …). Don't reintroduce a third profile.

2. **Flyway owns the schema.** `V1__init_food_requests.sql` runs on every startup; Hibernate is configured with `ddl-auto=validate` so any drift between the migration and the JPA entity fails fast at boot. Schema changes go in **a new `Vn__*.sql`** — never edit `V1`.

3. **The gateway is the trust boundary.** `GatewayHeadersFilter` reads `X-User-*` and stuffs them into request attributes — it does **not** validate JWTs (the gateway already did). This service must never be exposed directly to the public internet, or any client could spoof those headers.

4. **`GetFoodsByIds` is the donation-service hydration RPC.** It's backed by `findAllByIdIn(...)` with `@EntityGraph(attributePaths = "imageIds")` so it issues exactly one SQL `SELECT … WHERE id IN (?, …)` with the join collection eager-loaded. If you change this method, run `spring.jpa.show-sql=true` and confirm only one query fires for a multi-id call — don't reintroduce N+1.

5. **Image hydration is best-effort.** Read paths call `imageServiceClient.getImagesByCreator(foodId)` and inline the URLs into the response. If image-service is down, the food row still returns — `images: []` — and the user sees food details without thumbnails. Do **not** turn hydration failures into 5xx for read paths.

6. **Image upload is *not* best-effort.** `POST /` saves the food row first (to mint a UUID for `creator_id`), then uploads bytes to image-service. If the upload fails, the request returns 502 — the food row is meaningless without the images the user attached.

7. **Resilience annotations belong on private wrapper methods**, not on repository or stub calls directly. The service layer wraps DB calls in `@Retry(dbRetry) @CircuitBreaker(dbBreaker)` (reads only) and `@CircuitBreaker(dbBreaker)` (writes — never retry `save`, it's not idempotent). The gRPC client wraps every call in `@TimeLimiter + @Retry + @CircuitBreaker`.

8. **Two interceptors handle two error surfaces.**
   - REST: `GlobalExceptionHandler` (`@RestControllerAdvice`) → JSON `ErrorResponse`
   - gRPC: `GrpcExceptionInterceptor` (`@GrpcGlobalServerInterceptor`) → gRPC `Status`

   New exception types must be added to **both** mappings if the exception can surface on both surfaces.

9. **Proto contracts are shared.** `food-service.proto` and `image-service.proto` live in the [`proto-contracts`](https://maven.pkg.github.com/greengrub-team/proto-contracts) repo. To change them: edit there, push, merge, republish, then `./mvnw clean install -DskipTests -s settings.xml` here to refresh.

10. **Always use `-s settings.xml` for Maven.** Project-specific repo creds live there (for the GitHub Packages proto-contracts repo). The default `~/.m2/settings.xml` mirrors a different org repo and won't resolve some deps.

11. **Circuit-breaker state is observable.** `GET /actuator/health` shows `circuitBreakers.dbBreaker.state` and `circuitBreakers.imageBreaker.state` (`CLOSED` / `OPEN` / `HALF_OPEN`).

---

## Profiles

| Profile | Database | Image storage call | Use case |
| --- | --- | --- | --- |
| `local` | Postgres in Docker (`localhost:5432/greengrub`) | gRPC → `localhost:9095` | Local development |
| `k8s` | GCP Cloud SQL (Postgres, env-driven) | gRPC → `${IMAGE_SERVICE_HOST}:${IMAGE_SERVICE_PORT}` | GKE deployment |

`local` is the default (`spring.profiles.active=local` in `application.properties`).

---

## Key Config Files

| File | Purpose |
| --- | --- |
| `application.properties` | Shared base — app name, port `8081`, default profile, springdoc, actuator |
| `application-local.properties` | Postgres docker, gRPC server `9091`, image-service at `localhost:9095`, dev R4J thresholds, CORS for `:3000`/`:5173` |
| `application-k8s.yml` | Cloud SQL via env (`DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD`), image-service via env, prod R4J thresholds, CORS empty by default |
| `db/migration/V1__init_food_requests.sql` | Initial schema — `food_requests` + `food_request_image_ids` join table |

---

## REST API

Base path: `/api/v1/food-requests`

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/` | Create a food request (with optional image bytes). Returns 201 + hydrated `FoodRequestDTO` |
| `GET` | `/{id}` | Fetch a single food request, with `images[]` populated by image-service |
| `GET` | `/by-user/{userId}?page&size` | Paged list of foods posted by a user |
| `PATCH` | `/{id}/status` | Update status (`PENDING` → `APPROVED` → `DONATED` / `EXPIRED`) |
| `DELETE` | `/{id}` | Delete a food request (cascades the `food_request_image_ids` rows) |

Responses use the platform `ErrorResponse` shape on failure: `{timestamp, status, error, message, path}`.

---

## gRPC API

Listens on **port `9091`** (configurable via `grpc.server.port`).

| Method | Request | Description |
| --- | --- | --- |
| `CreateFood` | `CreateFoodRequest` | Create a food request |
| `GetFood` | `GetFoodRequest` | Fetch one food by id |
| `GetFoodsByUser` | `GetFoodsByUserRequest` | Paged list by user id |
| `GetFoodsByIds` | `GetFoodsByIdsRequest` | **Batch** lookup — single round trip + single SQL `IN (?, …)`. Used by donation-service to hydrate its `foodItemsId` list |
| `UpdateFoodStatus` | `UpdateFoodStatusRequest` | Update status |

All responses include the proto `Food.images` field, hydrated server-side from image-service.

Proto definitions live in the shared [`proto-contracts`](https://maven.pkg.github.com/greengrub-team/proto-contracts) repository.

---

## Schema (Flyway)

Two tables, owned by `V1__init_food_requests.sql`:

```
food_requests
  id              VARCHAR(36) PK     -- UUID (generated in @PrePersist)
  food_name       VARCHAR(255)
  quantity_amount NUMERIC(12, 2)     -- embedded Quantity.amount
  quantity_unit   VARCHAR(32)        -- embedded Quantity.unit (KG, SERVINGS)
  requested_by    VARCHAR(36)        -- user UUID from gateway
  requested_date  TIMESTAMP
  used_by_date    TIMESTAMP
  status          VARCHAR(32)        -- PENDING, APPROVED, DONATED, EXPIRED
  created_at      TIMESTAMP
  updated_at      TIMESTAMP

food_request_image_ids        -- @ElementCollection join table
  food_request_id VARCHAR(36) FK → food_requests.id  (ON DELETE CASCADE)
  image_id        VARCHAR(36)
  PK (food_request_id, image_id)
```

Indexes on `requested_by` and `status` (the two main query predicates).

---

## Resilience Patterns

We use **Resilience4j** to keep the service stable when Postgres or image-service misbehaves. All three patterns are applied where appropriate.

### Retry

Automatically retries failed operations caused by transient infrastructure issues.

| Instance | Applied to | Config (local / k8s) |
| --- | --- | --- |
| `dbRetry` | DB **read** helpers (`findById`, `findByUser`, `findAllByIdIn`) | 3 attempts, exponential backoff 200ms → 400ms → 800ms |
| `imageRetry` | image-service `uploadImages` and `getImagesByCreator` | 3 attempts, exponential backoff 1s → 2s → 4s |

**`save` is never retried** — JPA save is not idempotent under retry (a transient timeout could leave duplicates). Writes only get the breaker.

**Only infrastructure-class exceptions trigger image retries** — `ImageUploadFailedException`, `ImageHydrationFailedException`. Business exceptions (`FoodNotFoundException`, validation errors) propagate immediately.

### Circuit Breaker

Stops sending requests to a failing dependency, letting it recover instead of being overwhelmed.

| Instance | Applied to | local config | k8s config |
| --- | --- | --- | --- |
| `dbBreaker` | All DB calls | sliding=5, failure=60%, open=10s, half-open=2 | sliding=10, failure=50%, open=30s, half-open=3 |
| `imageBreaker` | All image-service gRPC calls | sliding=5, failure=60%, open=10s, half-open=2 | sliding=10, failure=50%, open=60s, half-open=3 |

**States:** `CLOSED` (normal) → `OPEN` (fail-fast, returns `CallNotPermittedException`) → `HALF_OPEN` (probing recovery)

When `imageBreaker` is open during a read, the service falls back to `images: []` and still returns the food row. When `dbBreaker` is open, the request returns 503.

### TimeLimiter

Cancels operations that take too long, preventing thread starvation.

| Instance | Applied to | Timeout |
| --- | --- | --- |
| `imageUploadLimiter` | `ImageServiceClient.uploadImages` | 10s |
| `imageReadLimiter` | `ImageServiceClient.getImagesByCreator` | 2s |

`@TimeLimiter` requires the wrapped method to return `CompletableFuture` — the gRPC client wraps the blocking stub in `CompletableFuture.supplyAsync(...)` to satisfy this. Reads use a tighter 2s budget because they're inline with user requests; uploads get 10s because they handle real bytes.

---

## Exception Handling

Two parallel handlers, one per surface:

- **REST**: `GlobalExceptionHandler` (`@RestControllerAdvice`) → JSON `ErrorResponse`
- **gRPC**: `GrpcExceptionInterceptor` (`@GrpcGlobalServerInterceptor`) → gRPC `Status`

### Mapping table

| Exception | REST status | gRPC status | When |
| --- | --- | --- | --- |
| `FoodNotFoundException` | 404 Not Found | `NOT_FOUND` | Lookup miss on `findById` / `existsById` |
| `MethodArgumentNotValidException` / `ConstraintViolationException` | 400 Bad Request | `INVALID_ARGUMENT` | Bean Validation failed on a request body |
| `DataIntegrityViolationException` | 409 Conflict | (not mapped — caught by REST only) | Postgres constraint violation |
| `ImageUploadFailedException` | 502 Bad Gateway | `UNAVAILABLE` | image-service rejected the upload, timed out, or breaker open |
| `CallNotPermittedException` (Resilience4j) | 503 Service Unavailable | `UNAVAILABLE` | A circuit breaker is OPEN |
| `ImageHydrationFailedException` | (not mapped — swallowed in service layer) | (n/a) | Hydration failed; service falls back to `images: []` |
| catch-all `Exception` | 500 Internal Server Error | `INTERNAL` | Unhandled — logged at ERROR with full stack |

### Where each exception is thrown

| Exception | Thrown when | Retried? |
| --- | --- | --- |
| `FoodNotFoundException` | `findById(id)` returns empty, or `delete(id)` on missing row | No |
| `ImageUploadFailedException` | image-service `UploadImages` gRPC call fails or times out | Yes (`imageRetry`) |
| `ImageHydrationFailedException` | image-service `GetImagesByCreator` gRPC call fails or times out | Yes (`imageRetry`) — and then swallowed |

### How the gRPC interceptor works

`GrpcExceptionInterceptor` is a global server interceptor (`@GrpcGlobalServerInterceptor` from `net.devh`). It wraps every incoming call's listener and translates any thrown exception:

1. Exception is thrown in the service layer (or by a Resilience4j wrapper)
2. If retryable → Resilience4j retries it (up to max attempts)
3. If retries are exhausted, or the breaker is open (`CallNotPermittedException`) → exception propagates
4. Interceptor catches it → maps to gRPC `Status` → returns a structured error to the caller

---

## Security & Identity

The gateway validates JWTs upstream and forwards three headers to this service:

- `X-User-Id` (UUID)
- `X-User-Email`
- `X-User-Role`

`GatewayHeadersFilter` copies them into request attributes so controllers can read them via `@RequestAttribute("userId") String userId`. The filter performs **no** signature checks — that's the gateway's job.

> **Trust boundary:** this service must never be exposed directly to the public internet in production. The gateway is the only legitimate caller. Locally, calling food-service directly on `:8081` will trust whatever `X-User-*` you send — that's expected for dev, dangerous for prod.

---

## CORS

`CorsConfig` is profile-driven via `app.cors.allowed-origins`:

- **local**: `http://localhost:3000,http://localhost:5173` — the React dev servers (CRA + Vite) so the frontend can call the service directly during development
- **k8s**: `${CORS_ALLOWED_ORIGINS:}` (empty default) — no `Access-Control-Allow-Origin` header emitted; gateway is the only legitimate external caller

Registered for `/api/**` only. Credentials are enabled iff origins are non-empty (never `*` with credentials).

---

## Observability

- `/actuator/health` — overall + circuit-breaker state for `dbBreaker` and `imageBreaker`
- `/actuator/info`
- `/actuator/metrics`, `/actuator/prometheus` — for scraping

---

## Running Locally

### Prerequisites

- Java 21
- Docker Desktop (for Postgres)
- Maven (use the `./mvnw` wrapper)
- A running [`image-service`](../image-service/) on `:9095` (for image upload + hydration)

### Steps

```bash
# 1. Start Postgres
docker run -d --name greengrub-postgres \
  -e POSTGRES_DB=greengrub \
  -e POSTGRES_USER=greengrub \
  -e POSTGRES_PASSWORD=greengrub \
  -p 5432:5432 \
  postgres:16

# 2. Start image-service in another terminal (see image-service README)

# 3. Run food-service
./mvnw spring-boot:run -s settings.xml
```

On first start, Flyway applies `V1__init_food_requests.sql` and creates the two tables. Subsequent starts validate the existing schema.

REST: `http://localhost:8081/api/v1/food-requests`
gRPC: `localhost:9091`
Actuator: `http://localhost:8081/actuator/health`

### Verify

```bash
# Health (both breakers should be CLOSED)
curl http://localhost:8081/actuator/health | jq .components.circuitBreakers

# REST smoke
curl -X POST http://localhost:8081/api/v1/food-requests \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 00000000-0000-0000-0000-000000000001' \
  -d '{
    "foodName": "Bananas",
    "quantity": {"amount": 2, "unit": "KG"},
    "requestedBy": "00000000-0000-0000-0000-000000000001"
  }'

# gRPC smoke (batch lookup)
grpcurl -plaintext \
  -d '{"food_ids":["<uuid1>","<uuid2>"]}' \
  localhost:9091 \
  com.greengrub.proto.foods.FoodService/GetFoodsByIds
```

With `spring.jpa.show-sql=true` (set in `application-local.properties`), the batch lookup should log exactly one `SELECT … WHERE id IN (?, ?)`.

---

## Building

```bash
# Compile (uses settings.xml for the GitHub Packages proto-contracts repo)
./mvnw clean compile -s settings.xml

# Refresh proto-contracts after a contracts repo change
./mvnw clean install -DskipTests -s settings.xml

# Build a runnable JAR
./mvnw clean package -s settings.xml
```

Always pass `-s settings.xml`. Project-specific repo creds live there.
