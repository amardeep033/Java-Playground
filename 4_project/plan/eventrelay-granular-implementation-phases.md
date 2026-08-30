# EventRelay — Granular AI-Assisted Implementation Plan

**Version:** 0.6 (supersedes 0.5)
**Stack:** Java 21, Spring Boot 3, Maven, Flyway, Spring AMQP, Spring Data Redis, Apache HttpClient 5, Micrometer + OpenTelemetry, JUnit 5 + Testcontainers (decision D1).
**Companion files:** `eventrelay-end-to-end-flow-master.md` (behavior, decisions) and `eventrelay-database-and-api-contract.md` (schema, APIs, messages).

### What changed in v0.6

- **Phase 16:** guards use the single claimable predicate (expired-lease `PROCESSING` rows included), with a test that strands a row, disables/pauses/expires it, and verifies it resolves and the reconciler does not loop.
- **Phase 21:** the fan-out app check (D33), tested for `DISABLED` and for a missing `apps_ref` row.
- **Phase 25:** failure suite now scenarios 1–31.

### What changed in v0.5

- **Phase 00:** seeds D1–D32, with the pending ones marked pending.
- **Phase 06/08:** `deliveries.webhook_id` in the schema; the sender takes `webhook-id` from it.
- **Phase 16:** pre-claim transitions are conditional updates (D28); the claim returns the previous status and writes the `LEASE_EXPIRED` history row on takeover; concurrent-duplicate-signal tests.
- **Phase 17:** retry-timing test asserts the maximum expected delay as well as "never early".
- **Phase 18:** field-by-field redrive reset test (D30), concurrent redrive test.
- **Phase 19:** the reconciler only signals; it no longer writes history.
- **Phase 25:** failure suite now scenarios 1–29. **Phase 27:** nginx routing table and a routing test per row.

### What changed in v0.4

- **Phase 05:** conflict target repeats the partial-index predicate; tested on real PostgreSQL, including concurrent identical keys and no key.
- **Phase 07:** pause/resume is a status flip only; the resume wake-up arrives with the broker in Phase 16.
- **Phase 15:** `EXISTS`-based fan-out selection with a dual-subscription test.
- **Phase 16:** resume handler (`next_retry_at = now()` plus signals), early signals preserve `next_retry_at`, with tests.
- **Phase 22:** health API returns a nested `projection` object.
- **Phase 25:** failure suite now scenarios 1–25.

### What changed in v0.3

Acceptance criteria and tests were moved into the phase where each behavior is built, instead of waiting for Phase 25:

- **Phase 02:** `IdGenerator` (UUIDv7, D24) and tests. **Phase 03:** prefixed-ID conversion tests at the HTTP boundary.
- **Phase 09:** the temporary REST link is explicitly dev-only, best-effort and unrecoverable until Phase 14 (D27), with tests.
- **Phase 15:** exact fan-out predicate, including `PAUSED` endpoints.
- **Phase 16:** the retry-due guard and the claim predicate, with early-signal and redelivery tests.
- **Phase 17:** `next_retry_at = now + tier TTL` rule tested.
- **Phase 18:** best-effort DLQ audit tests; replay API docs state the `webhook-id` reuse.
- **Phase 22:** health API source field and projection-vs-Redis tests.
- **Phase 25:** failure suite extended to scenarios 1–21. **Phase 27:** demo script shows the replay-dedupe behavior.

Unresolved decisions are listed in the flow document.

### What changed from v0.1

- 50 phases reduced to **28**. Each phase is one AI session, one commit, and one Git **tag**, so you can compare any two phases in VS Code.
- Order follows your learning path: two services with REST + DB first, then Redis, then RabbitMQ (replacing the temporary REST call), then retries/DLQ, then auth, then OTel.
- Tests are written **inside** every phase. The old "write unit tests" phase is gone.
- Throwaway work removed (the separate synchronous orchestration phase). The REST call between services is a documented temporary baseline that RabbitMQ replaces in Phase 14.
- Phases now cover what the readme asked for and v0.1 dropped: users/apps/API-key management, test event, health APIs, the `endpoint_health` projection, a minimal UI (optional), and the reconciler.
- Redis is introduced deliberately (rate limiter, concurrency cap, breaker), matching your study goal.

---

## How to work through each phase

1. **Inspect first.** Ask the AI to read the README, the three design files and the current code, with no edits.
2. **Plan.** It returns files to change, data flow, error paths and tests. You approve.
3. **Implement only that phase.** No unrelated refactors.
4. **Review the diff** in VS Code (checklist below) before running anything.
5. **Run** format, lint, tests; exercise the acceptance criteria with `curl`, SQL, the RabbitMQ UI or the local receiver.
6. **Ask the AI to explain the diff** only after you formed your own understanding.
7. **Commit and tag:** `git commit -m "<message>"` then `git tag phase-NN`.
8. **Log it** in `docs/phase-log.md`.

If the work reveals a flaw in the design files, stop and update them before changing code.

### Comparing phases in VS Code

```bash
git diff --stat phase-05..phase-06        # which files changed
git diff phase-05..phase-06 -- event-service/   # one service only
git show phase-06                          # the commit itself
git log --oneline --decorate               # tags next to commits
```
In VS Code: Source Control → "Compare with…" (or GitLens → "Compare References") and pick the two tags. Use "Open Changes" per file. Keep each phase's diff small enough to read in one sitting.

### Rules for AI-assisted coding

- One phase per prompt. Preserve existing behavior unless the phase changes it.
- Tests with every behavior; regression tests for anything changed.
- Explain every new dependency before adding it.
- No placeholder code, swallowed exceptions, empty catch blocks, or tests that assert nothing.
- No secrets in Git or logs. Never claim exactly-once webhook delivery.
- Never ACK a RabbitMQ message before its durable responsibility is complete (Flow Section 10).
- Never hold a DB transaction open across network I/O (broker, Redis, HTTP).
- No new infrastructure unless the phase names it.

### Prompt template (copy for every phase)

> We are implementing EventRelay. Read `README.md`, the three files in `docs/`, and the current code before proposing anything.
> **Phase:** [number and name] **Goal:** [one sentence]
> **In scope:** [exact changes] **Out of scope:** [what must not be built yet]
> **Constraints:** no unrelated refactors; do not silently change API/DB/message contracts; explain new dependencies first; add tests; no secrets in code or logs; no exactly-once claims.
> **Before editing:** return a concise plan (files, data flow, error paths, tests) and wait for approval.
> **After editing:** summarize each file change, explain the important lines, list the commands run and their results, and say what you did not verify.

### Diff-review checklist

- [ ] Is the scope exactly this phase?
- [ ] Can I explain every new file and public method?
- [ ] Is each piece in the correct service?
- [ ] Are HTTP, SQL, broker and business logic separated?
- [ ] Are transaction boundaries explicit, and none spanning network calls?
- [ ] Can a crash leave inconsistent durable state?
- [ ] Is anything critical only in memory?
- [ ] Are duplicate messages/requests safe?
- [ ] Are errors classified, not swallowed? Are secrets redacted and sizes limited?
- [ ] Do tests check behavior, not implementation details?
- [ ] Any unnecessary dependency or abstraction? Do the design files need an update?

### Phase log template

```markdown
### Phase NN — name
- Tag / commit:
- Goal:
- Files changed:
- New concepts:
- Data flow before → after:
- Failure cases tested:
- Commands run:
- What I can explain without AI:
- Questions to revisit:
- Design docs updated:
```

---

## Checkpoints

| Checkpoint | Phases | What works at the end |
|---|---|---|
| A — Event Service | 00–05 | Authenticated, idempotent event ingestion; events persisted |
| B — Two services over REST | 06–09 | Endpoints, subscriptions, signed delivery with attempt history (sync baseline) |
| C — Redis protections | 10–12 | Rate limits, breaker, concurrency cap, SSRF guard |
| D — Durable async | 13–16 | Outbox → RabbitMQ → fan-out → worker, replacing the REST link |
| E — Recovery | 17–19 | Tiered retries, DLQ, redrive, replay, test event, reconciler |
| F — Platform | 20–24 | JWT/RBAC, app management, health, metrics, traces, retention, rotation |
| G — Confidence and demo | 25–27 | Failure-injection suite, optional UI, deployment, demo script |

---

# Part A — Foundation

## Phase 00 — Project notebook and decision log
**Goal:** make the project understandable before any code.
**Build:** `README.md` (problem, features, non-goals, architecture sketch); `docs/` containing the three design files; `docs/decisions.md` (ADR template, seeded with D1–D33 from the flow doc; mark D22 (replay `webhook-id`), D24 (UUIDv7 library API) and the JWT key-distribution question as **PENDING**, not final); `docs/phase-log.md`; a glossary (event, endpoint, subscription, delivery, attempt, outbox, ACK, DLQ, redrive, replay, idempotency).
**Out of scope:** any code or infrastructure.
**Done when:** a new reader can explain the project in two minutes.
**Tag/commit:** `phase-00` — `docs: establish EventRelay notebook`

## Phase 01 — Two service skeletons, Postgres, health
**Goal:** a repeatable baseline.
**Build:** Maven parent with `event-service` and `delivery-service` (Spring Boot 3, Java 21); typed `@ConfigurationProperties` with startup validation (fail fast, never log full URLs or secrets); `docker-compose.yml` with one PostgreSQL instance creating two databases and users via an init script; Flyway baseline per service; Actuator liveness/readiness; `.editorconfig`, formatter, `.env.example` (names only).
**Out of scope:** business tables, broker, Redis.
**Done when:** `docker compose up -d` plus both apps start; both databases reachable independently; invalid config exits with a clear error; tests for config validation pass.
**Tag/commit:** `phase-01` — `chore: scaffold services and local Postgres`

# Part B — Event Service (REST + DB)

## Phase 02 — Event Service schema and repositories
**Goal:** versioned schema and persistence separated from HTTP.
**Build:** Flyway migrations for `users`, `apps`, `api_keys`, `events`, `outbox` with all constraints and indexes from the contract; domain types (ids, event type validation, payload limits); an `IdGenerator` interface backed by `uuid-creator` (UUIDv7, D24; pin the version, verify the method name, record it in `docs/decisions.md`); `EventRepository` (insert, find) using `JdbcClient`; Testcontainers integration tests (insert, lookup, duplicate PK, partial unique index behavior).
**Out of scope:** HTTP, auth, broker.
**Done when:** a fresh DB migrates; constraints reject invalid data; repository tests pass against real PostgreSQL; `IdGenerator` tests prove the version nibble is 7 and that IDs sort by creation millisecond.
**Tag/commit:** `phase-02` — `feat(event): schema and event repository`

## Phase 03 — `POST /v1/events` (no auth yet)
**Goal:** the first vertical slice.
**Build:** request/response DTOs; validation (`415`, `400`, `422`, `413`); the standard error format; persists the event only; returns `202`. A clearly marked temporary dev app ID until Phase 04. Add the HTTP-boundary ID component (D3): `evt_<uuid>` format/parse.
**Out of scope:** auth, idempotency, outbox.
**Done when:** `curl` stores one event and returns its ID; invalid inputs return the documented errors; a SQL query confirms the row; ID tests cover round-trip, wrong prefix, invalid UUID, uppercase UUID, and a raw UUID where a prefixed one is required (`404` for paths, `422` for body/query).
**Tag/commit:** `phase-03` — `feat(event): accept events`

## Phase 04 — API keys and authentication
**Goal:** identify the caller.
**Build:** an admin-only CLI/seed command to create a first user, app and API key (management APIs come in Phase 20); key format `er_<prefix>_<secret>`, SHA-256 hash, constant-time compare; auth filter for `/v1/events`; `401`/`403` rules; scopes; revocation; log redaction.
**Out of scope:** JWT, user APIs.
**Done when:** missing, malformed, unknown, revoked, expired and valid keys behave as specified; the raw key is never stored or logged (test asserts it).
**Tag/commit:** `phase-04` — `feat(event): API key authentication`

## Phase 05 — Idempotency and the outbox row
**Goal:** safe client retries, and durable publish intent.
**Build:** `Idempotency-Key` handling with `ON CONFLICT (app_id, idempotency_key) WHERE idempotency_key IS NOT NULL DO NOTHING` (the column is `app_id`; the predicate is required so PostgreSQL infers the partial unique index) and request-hash comparison; same key + same body → original `event_id` plus `Idempotent-Replayed: true`; different body → `409`. Insert the `outbox` row (`EventAccepted` envelope) in the **same transaction**. Concurrency test with simultaneous identical requests; rollback test proving no partial event/outbox state.
**Out of scope:** publishing to RabbitMQ.
**Done when:** sequential and concurrent duplicates create one event; every accepted event has exactly one outbox row; a Testcontainers test runs the exact statement on real PostgreSQL with two concurrent inserts of the same key (one inserts, one gets no row and takes the replay/409 branch) and with no key (never conflicts).
**Tag/commit:** `phase-05` — `feat(event): idempotent ingest with transactional outbox`

# Part C — Delivery Service (REST + DB) and a temporary REST link

## Phase 06 — Delivery domain, schema, repositories
**Goal:** make the lifecycle explicit before any worker.
**Build:** migrations for `endpoints`, `endpoint_subscriptions`, `event_copy`, `deliveries` (including `webhook_id`, D32), `delivery_attempts`, `delivery_status_history`, `endpoint_health` (and `apps_ref`, unused until Phase 21); domain types, status enum, a pure state-transition function with exhaustive tests (allowed and forbidden), failure classification (D9) as a pure function; repositories with transaction support.
**Out of scope:** HTTP handlers, outbound calls.
**Done when:** transition tests pass without infrastructure; repository integration tests pass; no cross-service foreign keys exist.
**Tag/commit:** `phase-06` — `feat(delivery): lifecycle, schema and repositories`

## Phase 07 — Endpoints and subscriptions
**Goal:** configure where webhooks go.
**Build:** endpoint CRUD, pause/resume (status flip only; the resume wake-up arrives with the broker in Phase 16), soft delete, `PUT` subscriptions (exact type or `*`); secret generation shown once and AES-GCM encrypted at rest; basic URL validation (HTTPS, parseable); secret rotation with the 24h previous-secret overlap. Authorization is a clearly marked temporary dev header until Phase 20.
**Out of scope:** sending, SSRF hardening (Phase 12).
**Done when:** normal responses and logs never expose secrets; matching logic is unit-tested independently of the DB.
**Tag/commit:** `phase-07` — `feat(delivery): manage endpoints and subscriptions`

## Phase 08 — Signed sender and local receiver
**Goal:** prove the outbound HTTP behavior in isolation.
**Build:** a tiny local receiver (records headers/body; switches for 2xx, 4xx, 5xx, slow, connection drop); the sender with the D22 header scheme (`webhook-id` is read from `deliveries.webhook_id`, never recomputed), HMAC over the exact bytes, connect/total timeouts, no redirects, 1 KB response cap; a structured result type (success, HTTP error, timeout, network error). Signature verification test using the receiver.
**Out of scope:** persistence, retries, SSRF checks.
**Done when:** the receiver verifies signatures; timeouts and connection errors are results, not exceptions; response capture is bounded.
**Tag/commit:** `phase-08` — `feat(delivery): signed webhook sender`

## Phase 09 — Persisted attempts and the temporary REST link
**Goal:** the first end-to-end flow, through REST only.
**Build:** the claim query with `lease_token`/`lease_until`; the guarded persist transaction (attempt row, delivery update, history, `endpoint_health`); fan-out as a service method; a **temporary** internal endpoint `POST /internal/v1/events` on Delivery Service that Event Service calls after commit (documented as replaced in Phase 14). Event acceptance must succeed even if this call fails.
**Non-production, by design (D27):** the link exists only under a `dev-sync` profile, is called after commit with a 2s timeout and outside the transaction, and has **no retry and no recovery**. A failed call means no delivery until Phase 14. Do not describe the system as reliable before then. Log failures at ERROR and count them in `temp_rest_failures_total`. Delete the link in Phase 14.
**Out of scope:** retries (a failure just ends in a recorded attempt and a retryable status), Redis, RabbitMQ.
**Done when:** `curl` event → receiver gets a signed request; event, delivery, attempt and history rows are inspectable; one endpoint failing does not affect another; a stale-lease write is rejected (test); a failing internal call returns `202` to the client and leaves the event row and its `PENDING` outbox row intact (test).
**Tag/commit:** `phase-09` — `feat: end-to-end delivery over temporary REST`

# Part D — Redis protections

## Phase 10 — Redis rate limiters
**Goal:** protect ingest and each endpoint.
**Build:** Redis in Compose; Lua token bucket; ingest limit by app returning `429` with `Retry-After`; per-endpoint send limit that parks work (Phase 16 completes parking; for now it records a skipped attempt-less retry state); fail-open policy with metric; Testcontainers tests including concurrent callers and Redis down.
**Done when:** a burst yields `429`; one app's limit never throttles another; Redis outage does not take ingestion down.
**Tag/commit:** `phase-10` — `feat: Redis token-bucket rate limits`

## Phase 11 — Circuit breaker and concurrency cap
**Goal:** stop hammering a sick endpoint and bound per-endpoint concurrency.
**Build:** the Redis breaker (D16) with `CLOSED/OPEN/HALF_OPEN`, doubling cooldown, single probe via `SET NX`; breaker-counting vs non-counting failures; per-endpoint concurrency semaphore; both exposed to the worker as a small interface. Breaker skips never consume an attempt and never dead-letter.
**Done when:** a flapping receiver opens, half-opens and closes the breaker in an integration test; concurrent workers see one probe; other endpoints are unaffected.
**Tag/commit:** `phase-11` — `feat(delivery): endpoint circuit breaker and concurrency cap`

## Phase 12 — SSRF guard
**Goal:** the service must not become a proxy into internal networks.
**Build:** scheme/port policy; DNS resolution validating every address (private, loopback, link-local, multicast, reserved, metadata); a custom HttpClient `DnsResolver` that connects to the validated IP while preserving Host/SNI; redirects disabled; `ALLOW_LOCAL_TARGETS` dev flag rejected at startup under the prod profile; errors that don't leak internal details.
**Done when:** tests cover IPv4, IPv6, hostnames resolving to private IPs, and a rebinding stub; production profile rejects `127.0.0.1` URLs; the dev flag lets the local receiver work.
**Tag/commit:** `phase-12` — `feat(security): SSRF-safe outbound requests`

# Part E — RabbitMQ and durable async (replace the REST link)

## Phase 13 — Topology and broker spike
**Goal:** understand the broker before using it.
**Build:** RabbitMQ with the management UI in Compose; declarables for every exchange and queue in the contract (including the four retry tiers with fixed TTL + DLX, `delivery.dlq`, `dlq.poison`); `docs/topology.md`; a temporary harness that publishes, consumes, ACKs, nacks, requeues, rejects and watches a retry-tier expiry route back to main; a startup check that every binding exists.
**Out of scope:** outbox, business consumers.
**Done when:** topology declares repeatedly without surprises; a rejected message lands in `dlq.poison`; a tier message returns to `delivery.main` after its TTL.
**Tag/commit:** `phase-13` — `feat(messaging): RabbitMQ topology and semantics spike`

## Phase 14 — Outbox publisher
**Goal:** reliably turn outbox rows into messages.
**Build:** the lease-based claim (`FOR UPDATE SKIP LOCKED`, short transaction); publish with confirms and `mandatory`; mark `PUBLISHED` only after confirm; backoff on failure; metrics; **remove the temporary REST link and its `dev-sync` profile** from Phase 09; the publisher now drains any `PENDING` outbox rows left from earlier phases (fan-out idempotency absorbs duplicates for events already delivered over REST). Consider resetting the dev database before this phase for a clean demo.
**Done when:** pending rows publish after RabbitMQ returns; a row is never `PUBLISHED` before confirm; a simulated crash after confirm produces a tolerated duplicate; batch size and concurrency are bounded.
**Tag/commit:** `phase-14` — `feat(event): publish transactional outbox`

## Phase 15 — Idempotent fan-out consumer
**Goal:** broker events become durable per-endpoint deliveries.
**Build:** the `events.in` listener with manual ACK; the exact fan-out predicate from the contract (3.3): endpoints with `status IN ('ACTIVE','PAUSED')` and a matching subscription (type or `*`); envelope validation (poison → `dlq.poison`); the fan-out transaction; publishing `DeliverWebhook` with confirms; **ACK only after all publishes are confirmed**; bounded backoff on nack.
**Done when:** replaying the same `EventAccepted` creates no duplicate deliveries; DB failure before commit leaves the message unACKed; a no-subscriber event ACKs cleanly; an event matching a `PAUSED` endpoint creates a `PENDING` delivery while a `DISABLED` endpoint gets none (test); an endpoint subscribed to both the exact type and `*` yields one row from the selection query and exactly one delivery (test); killing the service between commit and publish loses nothing (test).
**Tag/commit:** `phase-15` — `feat(delivery): idempotent fan-out consumer`

## Phase 16 — Worker consumer and guards
**Goal:** the durable worker path.
**Build:** the `delivery.main` listener (N consumers, prefetch 1); guards D2.1–D2.5 implemented as conditional updates using the single claimable predicate, so expired-lease `PROCESSING` rows are handled too (D28: history, audit copy and tier publish only when exactly one row changed); breaker, rate-limit and concurrency checks with **parking** (`RETRY_SCHEDULED` + `park_reason`, publish to a tier, no attempt consumed); the claim, send and guarded persist from Phase 09, with the claim predicate from the contract (`RETRY_SCHEDULED` claimable only when `next_retry_at <= now()`), returning the previous status so a takeover of an expired lease writes the `LEASE_EXPIRED` history row (D29); early-signal rules (re-publish a signal only; `next_retry_at` is preserved; no tolerance window); the resume handler for `ENDPOINT_PAUSED` deliveries (Flow F4); ACK rules from Flow Section 10.
**Done when:** an event submitted via curl reaches the receiver through RabbitMQ; duplicate and stale signals are dropped; a paused endpoint parks without burning attempts; resuming it sets `next_retry_at = now()` on its parked deliveries and signals them, so they deliver without waiting for the old 10-minute timer, and the old timer message later arrives as a harmless duplicate (test); repeated early signals never move `next_retry_at` (test); N concurrent duplicate signals for a paused, expired or disabled delivery produce exactly one transition, one history row, one published signal or audit copy, no consumed attempt and an unchanged `next_retry_at` (Testcontainers test with parallel consumers). A further test leaves a delivery `PROCESSING` with an expired lease, then disables its endpoint (and separately pauses it, and separately lets it pass `expires_at`): the next signal must dead-letter or park it, write the `LEASE_EXPIRED` row, and the reconciler must not loop on it. **Tests:** an early signal for a `RETRY_SCHEDULED` delivery is re-parked and makes no HTTP request; a signal redelivered after a crash between the DB commit and the retry publish makes no HTTP request until `next_retry_at`; a claim attempted before `next_retry_at` returns zero rows (race test with two workers).
**Tag/commit:** `phase-16` — `feat(delivery): queue-driven delivery worker`

# Part F — Retries, DLQ, replay, repair

## Phase 17 — Retry policy and tiers
**Goal:** explicit, tested retry behavior.
**Build:** the attempt-to-tier mapping, `Retry-After` handling (next tier ≥ hint, cap 1h), `max_attempts` including the first request; retryable vs permanent classification (D9); tests that inject a clock and verify the whole schedule, including that every park/retry writes `next_retry_at = now + tier TTL` before publishing. A timing test asserts the maximum expected delay: a new retry fires within its tier TTL plus a small tolerance, a `Retry-After` hint rounds up to the next tier (e.g. 61s → 10m), and nothing ever fires before `next_retry_at`.
**Done when:** a dead URL walks 10s → 1m → 10m → 1h → 1h; a permanent failure stops at once; counters (`attempt_count`, `total_attempts`) are correct.
**Tag/commit:** `phase-17` — `feat(delivery): tiered retries`

## Phase 18 — Dead letters, redrive, replay, test event
**Goal:** terminal failures are visible and recoverable.
**Build:** `DEAD_LETTERED` with `dead_reason`; a **best-effort** audit copy to `delivery.dlq` (D25: a failed publish logs, increments `dlq_audit_publish_failures_total`, and ACKs); expiry handling (72h) and disabled-endpoint handling; `POST /redrive`, `POST /replay`, `POST /endpoints/{id}/test` per Flow F1–F3; history rows with actor and reason; the delivery list/detail/attempts and `journey` read APIs.
**Done when:** redrive restores a dead delivery with a fresh budget, the same delivery ID, unchanged `total_attempts` and intact history, and a field-by-field test confirms the D30 reset (`dead_reason`, `completed_at`, `next_retry_at`, `park_reason`, `lease_token`, `lease_until` cleared); two concurrent redrives yield one `202` and one `409`; replay creates a linked delivery; both reject ineligible states (`409`) and expired payloads (`410`); the DB, not the DLQ, explains every dead letter; a forced audit-publish failure still ACKs, leaves the row `DEAD_LETTERED` and increments the metric (test). The replay API documentation and the README state that replays reuse `webhook-id` and a deduplicating receiver will ignore them.
**Tag/commit:** `phase-18` — `feat(delivery): DLQ, redrive, replay, test event`

## Phase 19 — Reconciler and lease recovery
**Goal:** nothing stays stuck.
**Build:** the 60-second repair job (Flow G): re-signal stale `PENDING`, overdue `RETRY_SCHEDULED`, and expired `PROCESSING` leases (signals only; the `LEASE_EXPIRED` history row is written by the worker that takes the lease over, D29); bounded batches; metrics. It is idempotent and does not schedule retries.
**Done when:** deleting a signal from `delivery.main` by hand results in eventual delivery; a killed worker's delivery recovers; two reconciler instances cause no double send.
**Tag/commit:** `phase-19` — `feat(delivery): reconciler for lost signals and expired leases`

# Part G — Platform

## Phase 20 — Users, JWT, RBAC, app and API-key APIs
**Goal:** a real management surface.
**Build (Event Service):** register/login (hashed passwords), RS256 JWT (1h), roles `ADMIN`/`DEVELOPER`; app and API-key management APIs; `AppUpserted` written to the outbox in the same transaction as app changes. **Delivery Service:** verify the JWT with the public key; replace the temporary dev header; ownership checks enforced at the service/repository layer.
**Done when:** unauthenticated access is `401`; cross-tenant access is `404`; `DEVELOPER` cannot touch another user's app; API keys still work only for ingest/read.
**Tag/commit:** `phase-20` — `feat(security): JWT, RBAC, app and key management`

## Phase 21 — App projection (`apps_ref`)
**Goal:** Delivery Service can authorize without calling Event Service.
**Build:** the `apps.sync` consumer with version-checked upserts; backfill command; tests for out-of-order and duplicate messages; use `apps_ref` for ownership checks and for the fan-out app check (D33: `DISABLED` app → keep `event_copy`, create no deliveries, ACK; a missing `apps_ref` row counts as `ACTIVE`; test both and the metric).
**Done when:** creating, renaming and disabling an app propagates; stale versions are ignored.
**Tag/commit:** `phase-21` — `feat(delivery): app projection for authorization`

## Phase 22 — Health APIs and metrics
**Goal:** see how the system is doing.
**Build:** `endpoint_health` already written in Phase 09's transaction; add `GET /endpoints/{id}/health` (live breaker state from Redis with `breaker_state_source`, the lagging projection as a nested `projection` object, D26) and `GET /apps/{id}/health`; Micrometer metrics from Flow Section 15 (low-cardinality only); a short runbook explaining each metric.
**Done when:** metrics move predictably during a success and a forced failure; no metric is labeled with IDs, URLs or app names; with the Redis breaker `OPEN` and a stale projection, the API reports `OPEN` from `live`; with Redis stopped it reports the projection value from `projection` (test).
**Tag/commit:** `phase-22` — `feat(observability): health APIs and metrics`

## Phase 23 — Structured logs and tracing
**Goal:** follow one event across everything.
**Build:** JSON logs with correlation, `event_id`, `delivery_id`, `attempt_no`; `traceparent` propagated through HTTP and AMQP headers; OpenTelemetry spans for ingest, outbox publish, consume, fan-out, attempt and persist; no payloads or secrets in spans; works without a collector in dev.
**Done when:** one trace links `curl` to the delivery attempt; sample logs contain no secrets (test).
**Tag/commit:** `phase-23` — `feat(observability): correlated logs and traces`

## Phase 24 — Retention and secret-rotation overlap
**Goal:** bounded storage and safe rotation.
**Build:** the retention job (30 days for events, `event_copy`, attempts, history; purge published outbox after 7 days) in bounded batches; verify the 24h previous-secret overlap signs with both secrets and then drops the old one; replay returns `410` after payload expiry.
**Done when:** cleanup is batched and does not lock tables; rotation never invalidates in-flight deliveries.
**Tag/commit:** `phase-24` — `feat(platform): retention and secret rotation`

# Part H — Confidence and demo

## Phase 25 — Failure-injection suite
**Goal:** prove the crash windows, one at a time.
**Build:** Testcontainers scenarios 1–31 from Flow Section 18 (18–31 cover the retry-due guard, paused-endpoint fan-out, DLQ audit failure, ID conversion, resume wake-up, partial-index idempotency, dual-subscription fan-out, repeated early signals, concurrent duplicate signals, the redrive field reset, lease-takeover history, nginx routing, expired-lease guard handling and disabled-app fan-out). Each states the expected DB rows, broker state, receiver requests and metrics. Use bounded polling instead of sleeps. Add a small load test (slow and failing receivers) recording machine specs, parameters and results; tune only from measurements.
**Done when:** all scenarios pass from a clean environment; no test claims atomicity between a DB write and a remote HTTP side effect.
**Tag/commit:** `phase-25` — `test: failure injection and load`

## Phase 26 — Minimal UI (optional; skip if short on time)
**Goal:** make the demo visual without building a product.
**Build:** a small static SPA served behind nginx: login, apps/keys, endpoints, events, a delivery detail page (attempts and history), redrive/replay/test buttons, endpoint health. Role-aware menu. No analytics screen; link to Grafana instead.
**Done when:** the full demo can be run through the UI.
**Tag/commit:** `phase-26` — `feat(ui): minimal management console`

## Phase 27 — Deployment, nginx, demo script
**Goal:** run the project reliably on your budget.
**Build:** production Dockerfiles (non-root, `-Xmx` tuned); prod Compose with nginx (TLS, body limit, coarse rate limit, the explicit routing table from contract 7.0 (regex locations for the overlapping `/v1/apps/{id}/endpoints|health` and `/v1/events/{id}/journey` paths, with a routing test per row, including negatives), blocked `/actuator/prometheus`, RabbitMQ/Redis not exposed); migration and backup notes; restart policies; secrets via environment/secret files. Sizing note for the ~₹1500 budget: two JVMs, PostgreSQL, RabbitMQ, Redis and nginx need roughly 2–4 GB RAM; keep a single Postgres instance, cap heaps (about 256 MB each), and use a hosted free-tier backend for OTel instead of self-hosting Grafana/Tempo if memory is tight. Final README, architecture diagram matching the code, guarantees/non-guarantees, and a 5–10 minute demo script: create endpoint → submit event → show event + outbox → show RabbitMQ → show delivery + attempts → verify signature → force 500 and watch retries → breaker isolation → redrive → replay against a deduplicating receiver and explain why it is ignored (same `webhook-id`) → show logs, metrics, trace.
**Done when:** a clean machine can follow the README; a restart loses no durable work; no secrets are committed.
**Tag/commit:** `phase-27` — `chore: deployment and demo`

---

## Interview material by phase

| Topic | Phases |
|---|---|
| Transactional outbox and the dual-write problem | 05, 14, 15 |
| Idempotency at three layers | 05, 15, 16 |
| Leases, guarded writes, stale workers | 09, 16, 19 |
| Tiered retry queues and why a single TTL queue fails | 13, 17 |
| ACK boundaries and crash windows | 15, 16, 25 |
| Circuit breaker, rate limits, bulkhead on Redis | 10, 11 |
| SSRF and webhook signing | 08, 12 |
| Redrive vs replay, DB as source of truth vs DLQ | 18 |
| CQRS read projection | 09, 21, 22 |

## Future work (one line each, not in v1)

Rust rewrite of the Delivery Service; CDC (Debezium) with Kafka replacing the outbox poller; ClickHouse for analytics; Temporal for user-defined workflows; gRPC between services.
