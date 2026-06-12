# fw-store-service

[← FoodWise platform overview](https://github.com/tapok332/foodwise-platform)

The catalog and discovery service for the **FoodWise** food-rescue marketplace — store profiles, menu, categories, combos, promos, reviews, and PostGIS-powered geo search.

![Java 25](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)
![Spring Boot 4.0.5](https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL + PostGIS](https://img.shields.io/badge/PostgreSQL_16_+_PostGIS_3.4-336791?logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-3.9-231F20?logo=apachekafka&logoColor=white)
![Port 8083](https://img.shields.io/badge/port-8083-lightgrey)

---

## Overview

`fw-store-service` is the heaviest catalog microservice in FoodWise. It owns the full store lifecycle —
from admin seed data through real-time user discovery — and is the single source of truth for all menu
and product catalog data consumed by other services via typed internal contracts.

**Gateway routes to this service:** `/stores/**`, `/categories/**`, `/menu-items/**`, `/promos/**`,
`/home/**`, `/media/**`

**Consumes/produces:** Kafka transactional outbox (producer only); `menu-item.updated` events on item create/update.

---

## Engineering Highlights

### PostGIS Geo Search

`stores.location` is stored as `GEOGRAPHY(Point, 4326)` (WGS84). All distance queries use native PostGIS
functions through `StoreRepository`, enabling accurate spherical distance calculation at the DB level:

```sql
-- StoreRepository.findNearbyStores
SELECT s.*, ST_Distance(s.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS distance
FROM stores s
WHERE ST_DWithin(s.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius)
ORDER BY distance
```

A dedicated GiST spatial index (`idx_stores_location`) ensures sub-millisecond filtering on large datasets.
The `@GetMapping("/stores/nearby")` endpoint in `HomeController` delegates to this native query through
`StoreService.findNearbyStores`.

### Validated Query Contract — Sort Whitelist (400 on Unknown Sort)

`StoreSearchParams.SortField` is a sealed enum with an explicit `parse()` factory:

```java
// StoreSearchParams.java
case "distance" -> DISTANCE;
case "rating"   -> RATING;
case "priceAsc" -> PRICE_ASC;
case "priceDesc"-> PRICE_DESC;
case "relevance"-> RELEVANCE;
default -> throw new IllegalArgumentException("Unknown sort: " + value);
```

`StoreController` catches this exception and maps it to `400 Bad Request`, preventing silent fallback
to arbitrary ordering. Same guard applies to unknown `group`/`type` combinations (`type` and `group` are
mutually exclusive — the compact record constructor throws on conflict).

### StoreType Enum + StoreGroup

`StoreType` (RESTAURANT, CAFE, BAKERY, GROCERY, SWEETS, OTHER) is grouped under `StoreGroup`
(FOOD_SERVICE, RETAIL). The grouping drives both `GET /stores?group=` and `GET /home/categories?group=`
filtering. `StoreType.typesIn(group)` resolves the set at query time, so adding a new type only requires
adding one enum constant.

### Typed Internal DTOs

`InternalStoreController` (not exposed through the gateway) returns `InternalStoreDto` and
`InternalMenuItemDto` records — no `ApiResponse` envelope, no public-DTO fields leaked to downstream
consumers. The unified `/internal/items/{itemId}` endpoint resolves against `menu_items` first, then
delegates to `surprisebox-service` if not found, presenting a single contract shape to `cart-service`.

### Timezone-Correct `isCurrentlyOpen`

`StoreEntity.isCurrentlyOpen(ZoneId zone)` receives the configured zone (from `STORE_TIMEZONE` /
`foodwise.store.timezone`) rather than relying on the JVM default, which is typically UTC in containers.
It correctly handles overnight business hours (e.g., 22:00 – 02:00).

### Transactional Outbox Pattern

`KafkaConfig` wires a `KafkaTemplate` with `enable.idempotence=true`, `acks=all`, and a `store-tx-`
transaction prefix. Menu item mutations publish `menu-item.updated` events through `OutboxPublisher`
(from `fw-common`) inside the same DB transaction, eliminating the dual-write problem.

### Money Value Object

`deliveryFee` and `minOrderAmount` on `StoreEntity` are stored as an embedded `Money` VO
(`amountMinor: BIGINT + currency: VARCHAR(3)`), consistent with the platform-wide Money modeling
convention. All prices travel in minor currency units.

---

## API

### Public Endpoints (via Gateway :8080)

| Method | Path | Key Query Params | Notes |
|--------|------|-----------------|-------|
| `GET` | `/stores` | `search`, `type`, `group`, `categoryId`, `categorySlug`, `latitude`, `longitude`, `minRating`, `maxDistance`, `openNow`, `priceLevel`, `sort`, `page`, `limit` | Paginated. `sort` whitelist: `distance`, `rating`, `priceAsc`, `priceDesc`, `relevance`. Unknown → 400. `limit` capped at 100. |
| `GET` | `/stores/{id}` | `latitude`, `longitude` | Optionally includes computed distance. |
| `GET` | `/stores/{storeId}/promos` | — | Active promos only. |
| `GET` | `/stores/{storeId}/menu/search` | `query` | Full-text menu item search within store. |
| `POST` | `/stores` | — | Admin only (`ROLE_ADMIN`). |
| `GET` | `/categories` | `group`, `type` (multi-value), `locale` / `Accept-Language` | Localized category name. |
| `GET` | `/categories/{slug}` | `locale` | Resolve by URL-friendly slug. |
| `POST` | `/categories` | — | Admin only. |
| `GET` | `/home/featured-stores` | — | Curated store list. |
| `GET` | `/home/stores/nearby` | `lat`, `lng`, `radius` (km, default 5) | PostGIS `ST_DWithin` query. |
| `GET` | `/home/boxes` | `lat`, `lng`, `radius` (m, default 100 000) | Surprise boxes with available stock near location. |
| `GET` | `/home/categories` | `group`, `type` | Home-screen category strip, same data as `/categories`. |
| `GET` | `/home/hero-images` | — | Hero banner URLs. |
| `GET` | `/home/category-icons` | — | Icon names for category chips. |

### Internal Endpoints (not exposed via Gateway)

| Method | Path | Contract | Consumers |
|--------|------|----------|-----------|
| `GET` | `/internal/stores/{storeId}` | `InternalStoreDto` | `fw-order-service`, `fw-cart-service` |
| `GET` | `/internal/menu-items/{itemId}` | `InternalMenuItemDto` | `fw-cart-service` |
| `GET` | `/internal/items/{itemId}` | `InternalMenuItemDto` | `fw-cart-service` (unified: menu item or surprise box) |

All internal endpoints return the raw DTO with no envelope.

---

## Events

The service is a **producer only** — no Kafka consumers are registered. Events are published via the
transactional outbox pattern (table `outbox_events`).

| Topic | Trigger |
|-------|---------|
| `menu-item.updated` | Menu item created or updated (`StoreMenuItemService`) |

Platform-level topics relevant to this service (consumed by others, not produced here):
`store.updated`, `store-favorite.added`, `store-favorite.removed` — these are defined in `EventTopics`
but produced by other services.

---

## Data Model

All tables live in the default PostgreSQL schema of the `foodwise_stores` database.

| Table | Purpose |
|-------|---------|
| `stores` | Store profiles; `location GEOGRAPHY(Point,4326)` + GiST index; `store_payload JSONB` for extended metadata; `delivery_fee_*` / `min_order_*` as Money minor-unit pairs |
| `categories` | Cuisine/type categories with `slug` (URL-safe, lower-kebab) and JSONB translations |
| `menu_sections` | Ordered sections within a store menu |
| `store_menu_items` | Individual menu items; price stored in minor units (`price_amount_minor` + `price_currency`) |
| `combos` | Bundled item sets with `savings` field |
| `combo_items` | Many-to-many join between `combos` and `store_menu_items` |
| `surprise_boxes` | Surprise boxes with `stock`, `pickup_from/to`, `retail_price_*` for discount calculation |
| `store_promos` | Active promotions with emoji, colors, priority |
| `store_reviews` | User reviews; `rating SMALLINT CHECK (1–5)` |
| `store_hours` | Per-weekday operating hours (supplement to `opens_at`/`closes_at`) |
| `hero_images` | Home-screen banner images |
| `category_icons` | Icon name map for category chips |
| `outbox_events` | Transactional outbox for Kafka publishing |
| `processed_events` | Idempotency dedup table |

**Flyway migrations:** `V1` — core store schema + spatial index; `V2` — content tables; `V3` — `StoreType` column + `slug`; `V4` — multilingual category seed (80 categories).

---

## Configuration

| Env Var | Default | Description |
|---------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/foodwise_stores` | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | `tapok332` (dev only) | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `admin` (dev only) | DB password — use `.env` in production |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `STORE_TIMEZONE` | `Europe/Kyiv` | Wall-clock zone for `isCurrentlyOpen`; maps to `foodwise.store.timezone` |
| `INTERNAL_SERVICE_SECRET` | _(required)_ | Shared secret for inter-service requests |
| `SERVICES_SURPRISEBOX_URL` | `http://surprisebox-service:8084` | Surprise box service base URL |
| `SWAGGER_ENABLED` | `false` | Enable Swagger UI (`/swagger-ui.html`) and `/v3/api-docs` |

Secrets (`SPRING_DATASOURCE_PASSWORD`, `INTERNAL_SERVICE_SECRET`) must never be committed.
See `.env.example` in the platform repository for the full template.

---

## Running

### Full stack (recommended)

```bash
# From repository root — starts gateway, all microservices, Postgres, Kafka
docker compose up -d
```

The gateway at `http://localhost:8080` routes all `/stores/**` traffic to this service on port 8083.

### Standalone

```bash
cd fw-store-service

# Requires Postgres 16 + PostGIS running locally (see docker-compose.yml for connection params)
./gradlew bootRun
```

Service available at `http://localhost:8083`. Flyway applies all migrations on startup;
`spring.jpa.hibernate.ddl-auto=validate` enforces schema consistency.

---

## Testing

```bash
cd fw-store-service
./gradlew test
```

Test coverage includes:

| Test class | Type | What it covers |
|-----------|------|---------------|
| `StoreSearchIntegrationTest` | `@SpringBootTest` + Testcontainers (PostgreSQL) | PostGIS geo queries, filter combinations |
| `CategoryControllerIT` | `@SpringBootTest` + Testcontainers | Slug resolution, locale negotiation, sort whitelist 400 |
| `InternalStoreControllerTest` | `@WebMvcTest` | No-envelope contract, typed DTO shape |
| `HomeControllerDiscountTest` | `@WebMvcTest` | Discount percentage calculation for surprise boxes |
| `StoreTypeTest` | Unit | `StoreType.typesIn(group)` grouping correctness |
| `CategoryServiceTest` | Unit | Category filter logic |
| `ComboServiceTest` / `MenuSectionServiceTest` | Unit | CRUD and cascade behavior |
| `StoreServiceCreateTest` | Unit | Store creation with Money VO |
| `SecurityConfigTest` | Unit | Admin endpoint authorization |
| `LocaleResolverTest` | Unit | `Accept-Language` / `?locale=` priority |
| `CategorySeedRunnerTest` | Unit | Seed runner idempotency |

Integration tests use Testcontainers (`org.testcontainers:postgresql:1.20.4`) — no H2 in-memory DB.

