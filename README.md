# Yolo USSD App — Microservices (Kotlin / Spring Boot)

MTN Rwanda "Yolo" USSD app (*154#), built as three independently-runnable Kotlin/Spring Boot
microservices.

## Services

| Service               | Port | Purpose                                              | Database        |
|------------------------|------|-------------------------------------------------------|------------------|
| `ussd-gateway-service`| 8080 | USSD entry point — session parsing, language, and menu routing | PostgreSQL (`ussd_gateway_db`) |
| `bundle-service`      | 8081 | Bundle catalog — full CRUD                            | PostgreSQL (`bundle_db`) |
| `payment-service`     | 8082 | Payment transactions — full CRUD                      | PostgreSQL (`payment_db`) |

Each service has its own `build.gradle.kts` (Gradle, Kotlin DSL) and runs independently — this
mirrors real microservices deployment, where each service is built, versioned, and deployed on
its own.

## Running locally

Each service is a standalone Gradle project. It includes a Gradle wrapper pinned to Gradle 8.14.3.
Use the included wrapper rather than a globally installed Gradle version: Gradle 9 is not
compatible with the Kotlin plugin version used by this project.

```bash
cd bundle-service
.\gradlew.bat bootRun  # Windows
# macOS/Linux: ./gradlew bootRun
```

All services use PostgreSQL. Create the following databases before starting the project:

```sql
CREATE DATABASE bundle_db;
CREATE DATABASE payment_db;
CREATE DATABASE ussd_gateway_db;
```

Update the PostgreSQL username and password in each service's
`src/main/resources/application.properties` file to match your local PostgreSQL setup.

Start the services in this order so the gateway can reach its dependencies:
1. `bundle-service` (port 8081)
2. `payment-service` (port 8082)
3. `ussd-gateway-service` (port 8080)

`bundle-service` seeds the bundle catalog on first startup. The catalog covers Gwamon', YOLO Voice,
YOLO Internet, Social Media Bundles, DesaDe, FoLeva, and Redeem Loyalty Points merchants.

## IntelliJ note

Open each service folder separately in IntelliJ (`File → Open`, pointing at the `bundle-service`,
`payment-service`, or `ussd-gateway-service` folder) — IntelliJ detects `build.gradle.kts`
automatically and imports the Gradle project, same as it did with `pom.xml` before.

## Testing the CRUD APIs directly

**bundle-service** (`http://localhost:8081/api/bundles`):
- `GET /api/bundles` — list everything
- `GET /api/bundles?category=gwamon` — bundles under a root-level category
- `GET /api/bundles?category=yolo-internet&subcategory=daily` — bundles under a sub-branch
- `GET /api/bundles?category=gwamon&purchasableOnly=true` — only active bundles, with the
  Gwamon' Weekend day-of-week restriction applied
- `GET /api/bundles/{id}` — one bundle
- `POST /api/bundles` — create (see `BundleRequest` fields)
- `PUT /api/bundles/{id}` — update
- `DELETE /api/bundles/{id}` — delete

**payment-service** (`http://localhost:8082/api/payments`):
- `POST /api/payments` — initiate a payment attempt; returns the same outcome messages seen in
  the real *154# flow (Airtime insufficient / MoMo processing / Iherereze not allowed) — this is
  a STUB, see the TODO comment in `PaymentTransactionService.resolveOutcome()`
- `GET /api/payments` — list all, or filter with `?phoneNumber=` / `?ussdSessionId=`
- `GET /api/payments/{id}` — one transaction
- `PUT /api/payments/{id}/status?status=SUCCESS` — update status (e.g. once a real MoMo callback exists)
- `DELETE /api/payments/{id}` — delete

**ussd-gateway-service** (`http://localhost:8080/ussd`):
```bash
# Start a new USSD session
curl -X POST http://localhost:8080/ussd \
  -d "requestId=1&sessionId=my-session-001&phoneNumber=250780000000&text=&serviceCode=*154#"

# Continue the USSD session using the same sessionId
curl -X POST http://localhost:8080/ussd \
  -d "requestId=0&sessionId=my-session-001&phoneNumber=250780000000&text=0&serviceCode=*154#"
```
- `requestId=1` — starts a new session. Supply a unique `sessionId`; it is returned in the `X-USSD-Session-Id` response header.
- `requestId=0` — continues an existing session; it expires after five minutes of inactivity.
- `requestId` must be `1` or `0`; `phoneNumber` must contain 7–15 digits; and `sessionId` is required when `requestId=0`. Invalid requests return HTTP `400` with an `END Invalid request` message.
- `text=""` (root menu), `text="0"` (Gwamon' bundles, fetched live from bundle-service),
- `text="0*1"` (payment menu), `text="0*1*2"` (pay via MoMo — actually calls payment-service and
  returns its real response).

The gateway manages the full menu structure, including English/Kinyarwanda language selection,
invalid-option messages, unfinished-session resume prompts, balance check, YOLO Star, and payment
menus. Bundle selections are retrieved from `bundle-service`, while Airtime, MoMo, and Ihereze
payment outcomes are routed through `payment-service`.

## What's intentionally still missing

- **Balance check** and **YOLO Star** use demonstration responses because they need real subscriber,
  membership, and loyalty data from MTN systems.
- **MoMo's real async outcome** (approval flow + SMS) isn't wired up — `payment-service` records
  a `PROCESSING` status and stops there, same as the real behavior you captured in testing.
- No authentication/API-key protection between services yet — fine for local development, but
  worth adding (e.g. Spring Security + API keys, or mutual TLS) before this touches anything real.
