# saryom-food-service

Surplus/free **food sharing** for the Saryom marketplace — the "Share Food, Reduce
Waste" vertical. Givers post food they'd otherwise throw away; nearby takers browse,
reserve, and collect it.

- **Stack:** Java 25, Spring Boot 4.0.x (MVC), Spring Data JPA + Flyway (Postgres
  schema `food`), Spring Security (Firebase ID tokens), Spring Cloud Stream
  (Kafka/RabbitMQ), springdoc OpenAPI.
- **Port:** `8086`
- **Docs:** Swagger UI at `/swagger-ui.html`, spec at `/v3/api-docs`.

## Domain

A `FoodPost` is the aggregate root; its lifecycle is encapsulated on the entity so
invalid transitions are impossible from outside:

```
AVAILABLE --reserve--> RESERVED --complete--> COMPLETED
    |                     |
    |                     +--release--> AVAILABLE
    +--cancel--> CANCELLED   (also reachable from RESERVED)
```

Expiry is derived from `bestBefore` (posts past their collect-by time are excluded
from browse), so no scheduler is needed.

## API

Public browse; everything else needs `Authorization: Bearer <Firebase ID token>`
(or `dev:<uid>` under the `dev` profile). Enforced in the service layer.

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/food` | public | Browse available food (`q`, `foodType`, `sort`, `lat`/`lng`/`radiusMiles`, `page`, `size`) |
| GET | `/api/food/{id}` | public | Post detail |
| GET | `/api/food/mine` | ✓ | Posts I'm giving |
| GET | `/api/food/reserved` | ✓ | Posts I've reserved |
| POST | `/api/food` | ✓ | Share food |
| PATCH | `/api/food/{id}` | giver | Edit (while available) |
| POST | `/api/food/{id}/reserve` | ✓ | Reserve as taker |
| POST | `/api/food/{id}/release` | taker/giver | Release a reservation |
| POST | `/api/food/{id}/complete` | giver | Confirm collected |
| DELETE | `/api/food/{id}` | giver | Withdraw the post |

## Events (Spring Cloud Stream)

| Topic | When | Consumed by |
|-------|------|-------------|
| `food.posted` | food shared | notification-service (future: nearby alerts) |
| `food.claimed` | taker reserves | notification-service → alerts the giver |

## Local run

```bash
# Postgres schema `food` must exist (see saryom-db). Then:
mvn spring-boot:run -Dspring-boot.run.profiles=dev,rabbit
```

Under `dev`, tokens are `dev:<uid>` (stub verifier); with real Firebase, unset the
`dev` profile and provide `FIREBASE_SERVICE_ACCOUNT_JSON`.

## Deploy (Render)

`render.yaml` is a Blueprint (`autoDeploy: true`). First deploy is a one-time setup
in the Render dashboard: create the service from the blueprint and enter the
`DB_URL` / `DB_USER` / `DB_PASSWORD` / `CLOUDAMQP_URL` secrets, plus a `food` schema
and role in Neon (see saryom-db).
