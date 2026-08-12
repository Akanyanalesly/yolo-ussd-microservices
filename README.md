# Yolo USSD App — Microservices (Kotlin / Spring Boot)

MTN Rwanda "Yolo" USSD app (*154#), built as three independently-runnable Kotlin/Spring Boot
microservices.

## Services

| Service               | Port | Purpose                                              | Database        |
|------------------------|------|-------------------------------------------------------|------------------|
| `ussd-gateway-service`| 8080 | USSD entry point — session parsing, menu routing      | none (stateless) |
| `bundle-service`      | 8081 | Bundle catalog — full CRUD                            | H2 (in-memory)   |
| `payment-service`     | 8082 | Payment transactions — full CRUD                      | H2 (in-memory)   |

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

Start them in this order so the gateway can reach its dependencies:
1. `bundle-service` (port 8081)
2. `payment-service` (port 8082)
3. `ussd-gateway-service` (port 8080)

Both `bundle-service` and `payment-service` default to an **in-memory H2 database**, so they
run immediately with zero setup — no PostgreSQL install needed to try this out. `bundle-service`
also seeds itself on first startup with the full bundle catalog mapped from the real *154# menu
(Gwamon', YOLO Voice, YOLO Internet, Social Media Bundles, DesaDe, FoLeva, and the Redeem Loyalty
Points merchants).

To switch either service to real PostgreSQL: open its `src/main/resources/application.properties`,
comment out the H2 block, and uncomment the PostgreSQL block (update credentials as needed), then
create the matching database (e.g. `createdb bundle_db`).

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
  -d "requestId=1&sessionId=&phoneNumber=250780000000&text=&serviceCode=*154#"

# Continue an existing USSD session (use the sessionId returned from the new session request)
curl -X POST http://localhost:8080/ussd \
  -d "requestId=0&sessionId=<GENERATED_SESSION_ID>&phoneNumber=250780000000&text=0&serviceCode=*154#"
```
- `requestId=1` — generates a new UUID-based session ID, returned in the `X-USSD-Session-Id` response header
- `requestId=0` — continues an existing session using the generated session ID; returns session-expired error if the session is invalid or older than 5 minutes
- `requestId` must be `1` or `0`; `phoneNumber` must contain 7–15 digits; and `sessionId` is required when `requestId=0`. Invalid requests return HTTP `400` with an `END Invalid request` message.
- `text=""` (root menu), `text="0"` (Gwamon' bundles, fetched live from bundle-service),
- `text="0*1"` (payment menu), `text="0*1*2"` (pay via MoMo — actually calls payment-service and
  returns its real response).

## Important scope note

This zip demonstrates the **integration pattern**, not a full re-implementation of every branch.
Across our earlier conversation, every menu branch (YOLO Voice, YOLO Internet's five durations,
Social Media Bundles, root DesaDe, FoLeva, Balance check, YOLO Star and its Redeem Loyalty Points
sub-tree, language toggle) was built and tested as **hardcoded logic** directly inside
`ussd-gateway-service`, one VS Code AI prompt at a time.

This project instead fully wires up **one branch — Gwamon'** — end-to-end through real
`bundle-service` and `payment-service` calls, as the reference pattern. Every other root option
in `UssdController.kt` is a clearly labeled stub explaining it still needs migrating. To migrate
a branch:
1. Make sure `DataSeeder.kt` in `bundle-service` already has that branch's bundles seeded (most
   are already there — check the comments).
2. In `ussd-gateway-service`, replace the branch's stub with the same three-function pattern used
   for Gwamon': `buildBundleMenu()`, `buildPaymentMenu()`, `handlePaymentSelection()`, calling
   `bundleServiceClient.getByCategory(...)` or `getByCategoryAndSubcategory(...)` as appropriate.
3. For branches with an extra menu level (YOLO Internet, Social Media Bundles, Redeem Loyalty
   Points), add one more level of `when` dispatch before reaching the bundle menu — same idea as
   the existing `handleGwamon()`, just one level deeper.

## What's intentionally still missing

- **Balance check**, **YOLO Star** (membership/account/loyalty points), and **language
  preference** all depend on real subscriber state, which belongs in a future `user-service` /
  `session-service` — not `bundle-service`. These remain stubs by design.
- **MoMo's real async outcome** (approval flow + SMS) isn't wired up — `payment-service` records
  a `PROCESSING` status and stops there, same as the real behavior you captured in testing.
- No authentication/API-key protection between services yet — fine for local development, but
  worth adding (e.g. Spring Security + API keys, or mutual TLS) before this touches anything real.
