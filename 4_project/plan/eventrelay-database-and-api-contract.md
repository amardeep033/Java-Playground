# EventRelay — Database Tables and API Contract

**Version:** 0.6 (supersedes 0.5)
**Depends on:** `eventrelay-end-to-end-flow-master.md` (decision IDs `D1…D23` refer to its Section 0)
**Note:** This is a contract, not a migration script. Translate it into Flyway migrations (one set per service) and review before use.

### What changed in v0.6

- **Claimable predicate** (3.5) is now the single definition used by the claim, every pre-claim dead-letter and every park; it includes expired-lease `PROCESSING` rows (D28).
- **`apps_ref` use in fan-out** documented (3.1, D33): `DISABLED` → no deliveries; missing row = `ACTIVE`.

### What changed in v0.5

- **Routing table (7.0)** for the overlapping `/v1/apps/...` and `/v1/events/...` paths; the old prefix summary is removed (and the non-existent `/v1/health` prefix with it).
- **`deliveries.webhook_id`** added (3.5, D32).
- **History policy** (3.7, D29): same-status `LEASE_EXPIRED` rows on lease takeover; no row for a park that doesn't change status.
- Redrive reset and pre-claim conditional transitions are specified in the flow (D28, D30); the claim returns the previous status.

### What changed in v0.4

- **Idempotency conflict target** repeats the partial-index predicate (2.4).
- **Fan-out predicate** uses `EXISTS` for per-endpoint deduplication (3.3).
- **Health API shape:** the projection is a nested `projection` object; top-level `breaker_state` is live (3.8, 7.2, D26).
- **Resume behavior** documented on the endpoint API (7.2); details in flow F4.

### What changed in v0.3

- **IDs (flow D3, D24):** explicit boundary rule below; message examples now use raw UUIDs.
- **Fan-out predicate** specified exactly (section 3.3): `ACTIVE` and `PAUSED`, never `DISABLED`.
- **Claim predicate** documented with the due-time check (section 3.5).
- **DLQ audit copies are best-effort** (6.5, flow D25).
- **`endpoint_health`** is a lagging projection; the health API shape now carries `breaker_state_source` and `last_recorded_*` fields (3.8, 7.2).
- Replay API documentation now states the `webhook-id` reuse (7.2).
- Idempotency columns are `app_id` everywhere (confirmed against the partial unique index in 2.4).
- Unresolved decisions are listed in the flow document ("Decisions still unresolved after v0.3").

### What changed from v0.1

- Tables reduced to what the flow actually uses. Removed: `idempotency_records` (columns on `events`), `delivery_replay_requests` (audit via `delivery_status_history`), `endpoint_circuit_breakers` (Redis), optional `processed_messages` and `application_rate_limits`.
- Added: `users`, `endpoint_subscriptions` (renamed), `event_copy`, `delivery_status_history`, `endpoint_health` (CQRS read projection), `apps_ref`.
- `deliveries`: new status vocabulary, `lease_token`, `redrive_count`, `total_attempts`, `replay_of`, `park_reason`, `expires_at`; payload moved to `event_copy`.
- Auth model split (API key for ingest only; JWT for management) and APIs rewritten accordingly.
- Added missing APIs: register/login, app and API-key management, subscription replace, pause/resume, test event, redrive, health.
- Message contracts and RabbitMQ topology specified (tiered retry, poison DLQ).

---

## 1. Ownership rules

- Two databases on one PostgreSQL instance: `eventrelay_event` (Event Service) and `eventrelay_delivery` (Delivery Service). Separate credentials; no cross-database queries, no cross-database foreign keys. Cross-service references are plain IDs (D2).
- All timestamps are `timestamptz` in UTC.
- **ID representation (D3, D24):** database columns are `uuid`, generated as UUIDv7 through the `IdGenerator` interface. Internal RabbitMQ messages, JWT `sub`, logs and metrics carry the raw canonical lowercase hyphenated UUID. The public API uses `prefix_<uuid>` (`evt_`, `del_`, `ep_`, `app_`, `key_`). Conversion happens only in one HTTP-boundary component. A wrong prefix or invalid UUID in a path → `404`; in a body or query → `422`; uppercase UUIDs are rejected (canonical lowercase only). Tests cover round-trips and every rejection case.
- Secrets are never stored in plaintext. API keys are hashed (SHA-256 of a high-entropy random key); signing secrets are AES-GCM encrypted with a key from the environment.
- RabbitMQ is transport only; Redis is ephemeral only.

---

## 2. Event Service database (`eventrelay_event`)

### 2.1 `users`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| email | text, unique, lowercased | |
| password_hash | text | bcrypt/argon2, never returned |
| role | text | `ADMIN`, `DEVELOPER` |
| created_at | timestamptz | |

### 2.2 `apps`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| owner_user_id | uuid FK → users | |
| name | text | |
| status | text | `ACTIVE`, `DISABLED` |
| rate_limit_per_sec | int | default 20 (ingest token bucket refill) |
| burst | int | default 40 |
| version | bigint | incremented on every change; used by `AppUpserted` |
| created_at, updated_at | timestamptz | |

Index: `(owner_user_id)`.

### 2.3 `api_keys`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| app_id | uuid FK → apps | |
| name | text | |
| key_prefix | text, **unique** | non-secret lookup part of `er_<prefix>_<secret>` |
| key_hash | bytea | SHA-256 of the full key; compared in constant time |
| scopes | text[] | `events:write`, `events:read` |
| status | text | `ACTIVE`, `REVOKED` |
| expires_at, last_used_at, revoked_at | timestamptz null | |
| created_at | timestamptz | |

The raw key is returned once at creation. `last_used_at` is updated at most once per minute per key to avoid write amplification.

### 2.4 `events`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | public `evt_…` |
| app_id | uuid FK → apps | |
| event_type | text | |
| payload | jsonb | ≤ 256 KB; immutable |
| occurred_at | timestamptz null | caller-supplied |
| accepted_at | timestamptz | server time |
| idempotency_key | text null | |
| request_hash | bytea null | SHA-256 of canonical `{type,data,occurred_at}` |
| schema_version | int | |
| correlation_id | text | |

Constraints and indexes:
- **Partial unique index** `(app_id, idempotency_key) WHERE idempotency_key IS NOT NULL`. The insert must repeat the predicate in its conflict target: `ON CONFLICT (app_id, idempotency_key) WHERE idempotency_key IS NOT NULL DO NOTHING`; otherwise PostgreSQL cannot infer the partial index and the statement fails.
- `(app_id, accepted_at DESC, id DESC)` for cursor pagination; `(app_id, event_type, accepted_at DESC)` for filtered lists.
- Never updated after insert. Rows older than 30 days are deleted by the retention job (D20).

### 2.5 `outbox`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | becomes the AMQP `message_id` |
| message_type | text | `EventAccepted`, `AppUpserted` |
| aggregate_type, aggregate_id | text, uuid | `EVENT`/`APP` and its ID |
| schema_version | int | |
| payload | jsonb | the full versioned envelope (Section 6) |
| status | text | `PENDING`, `IN_FLIGHT`, `PUBLISHED` |
| attempt_count | int | publisher attempts |
| next_attempt_at | timestamptz | |
| claimed_by | text null | publisher instance |
| claim_until | timestamptz null | lease expiry |
| last_error | text null | sanitized, ≤ 500 chars |
| created_at, published_at | timestamptz | |

Indexes: partial `(next_attempt_at) WHERE status='PENDING'`, partial `(claim_until) WHERE status='IN_FLIGHT'`. Published rows older than 7 days are purged.

---

## 3. Delivery Service database (`eventrelay_delivery`)

### 3.1 `apps_ref` (projection of Event Service apps)
| Column | Type | Notes |
|---|---|---|
| app_id | uuid PK | |
| owner_user_id | uuid | for authorization |
| status | text | |
| version | bigint | upsert only if incoming version is higher |
| updated_at | timestamptz | |

**Use in fan-out (D33):** if the app's `status` is `DISABLED`, fan-out keeps the `event_copy`, creates no deliveries and ACKs. A missing row is treated as `ACTIVE`.

### 3.2 `endpoints`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | public `ep_…` |
| app_id | uuid | no FK (cross-service) |
| name | text | |
| url | text | HTTPS; validated on write and again at send time |
| status | text | `ACTIVE`, `PAUSED`, `DISABLED` |
| secret_ciphertext | bytea | AES-GCM |
| secret_version | int | |
| prev_secret_ciphertext | bytea null | overlap window (D18) |
| prev_secret_expires_at | timestamptz null | |
| max_attempts | int | default 6 |
| rate_limit_per_sec | int | default 10 |
| max_concurrency | int | default 4 |
| connect_timeout_ms, timeout_ms | int | defaults 3000, 10000 |
| created_at, updated_at | timestamptz | |

Index: `(app_id, status)`.

### 3.3 `endpoint_subscriptions`
`(endpoint_id uuid FK, event_type text, created_at)`, PK `(endpoint_id, event_type)`. Index `(event_type, endpoint_id)` for fan-out. `event_type = '*'` means all types. The subscription set is replaced as a whole through the API.

**Fan-out selection predicate (exact):** `SELECT e.* FROM endpoints e WHERE e.app_id = :app_id AND e.status IN ('ACTIVE','PAUSED') AND EXISTS (SELECT 1 FROM endpoint_subscriptions s WHERE s.endpoint_id = e.id AND s.event_type IN (:event_type, '*'))`. `EXISTS` (not a join) guarantees one row per endpoint even when it subscribes to both the exact type and `*`. `PAUSED` endpoints get a `PENDING` delivery that the worker parks without consuming an attempt; `DISABLED` endpoints get none.

### 3.4 `event_copy`
| Column | Type | Notes |
|---|---|---|
| event_id | uuid PK | |
| app_id | uuid | |
| event_type | text | |
| payload | jsonb | stored once, referenced by every delivery |
| schema_version | int | |
| source | text | `API`, `TEST` |
| correlation_id | text | |
| accepted_at, received_at | timestamptz | |
| expires_at | timestamptz | received_at + 30 days |

### 3.5 `deliveries`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | public `del_…` |
| event_id | uuid FK → event_copy | |
| endpoint_id | uuid FK → endpoints | |
| app_id | uuid | denormalized for scoping and listing |
| status | text | `PENDING`, `PROCESSING`, `RETRY_SCHEDULED`, `SUCCESS`, `DEAD_LETTERED` |
| dead_reason | text null | `RETRIES_EXHAUSTED`, `PERMANENT_FAILURE`, `EXPIRED`, `ENDPOINT_DISABLED` |
| attempt_count | int | attempts in the current cycle (reset on redrive) |
| total_attempts | int | lifetime; source of `attempt_no` |
| max_attempts | int | snapshot at creation |
| redrive_count | int | |
| next_retry_at | timestamptz null | |
| park_reason | text null | `BREAKER_OPEN`, `RATE_LIMITED`, `ENDPOINT_PAUSED`, `CONCURRENCY_CAP` |
| expires_at | timestamptz | created_at + 72h; reset on redrive |
| lease_token | uuid null | guards all post-send writes (D14) |
| lease_until | timestamptz null | |
| last_http_status | int null | |
| last_error_code | text null | |
| last_error | text null | sanitized, ≤ 500 chars |
| replay_of | uuid null FK → deliveries | |
| webhook_id | uuid | value sent as the `webhook-id` header (D32). Equals `event_id` for normal deliveries; a replay copies `event_id` under the current pending decision (D22). Not unique. |
| created_at, updated_at, completed_at | timestamptz | |

Constraints and indexes:
- **Unique `(event_id, endpoint_id) WHERE replay_of IS NULL`**: one logical delivery per event and endpoint.
- `(app_id, created_at DESC, id DESC)`, `(endpoint_id, created_at DESC)`, `(event_id)` for listings.
- Reconciler: `(status, updated_at)` and `(status, next_retry_at)`; lease recovery: partial `(lease_until) WHERE status='PROCESSING'`.
- Legal transitions are enforced in domain code and tested (Flow Section 11).
- **Claim predicate:** `status='PENDING' OR (status='RETRY_SCHEDULED' AND next_retry_at<=now()) OR (status='PROCESSING' AND lease_until<now())`. A `RETRY_SCHEDULED` row is never claimable before `next_retry_at`. This is the single **claimable** predicate (D28): the claim, every pre-claim dead-letter and every park use it, so an expired-lease `PROCESSING` row can never be stranded by a guard that ignores it. Every park or retry writes `next_retry_at = now() + <tier TTL>` before publishing.

### 3.6 `delivery_attempts`
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| delivery_id | uuid FK | |
| attempt_no | int | `total_attempts` after the attempt |
| started_at | timestamptz | |
| duration_ms | int | |
| outcome | text | `SUCCESS`, `HTTP_ERROR`, `TIMEOUT`, `NETWORK_ERROR`, `TLS_ERROR`, `SSRF_BLOCKED` |
| http_status | int null | |
| error_code | text null | stable category |
| error_summary | text null | sanitized, ≤ 500 chars |
| response_excerpt | text null | ≤ 1 KB, redacted |
| breaker_state | text | state when sent: `CLOSED`/`HALF_OPEN` |

Unique `(delivery_id, attempt_no)`. A row is written only after an outcome is known. A crash mid-send leaves no row; the same-status `PROCESSING → PROCESSING` history row with reason `LEASE_EXPIRED`, written by the worker that takes the lease over (D29), records the uncertainty.

### 3.7 `delivery_status_history`
`(id bigserial, delivery_id, from_status, to_status, reason, actor, at)`. `actor` is `system` or a user ID (redrive/replay/test). One row per status change, plus one same-status `PROCESSING → PROCESSING` row with reason `LEASE_EXPIRED` for each lease takeover (D29). A park that leaves the status unchanged writes no row. Index `(delivery_id, at)`.

### 3.8 `endpoint_health` (CQRS read projection)
`(endpoint_id PK, breaker_state, consecutive_failures, last_success_at, last_failure_at, last_http_status, total_success, total_failure, updated_at)`. Updated in the same transaction as the attempt row, so it can never disagree with the attempts. It is rebuildable from `delivery_attempts`. Rolling-window rates come from Prometheus, not this table.

**It is a projection.** The Redis breaker and this table cannot be updated atomically together, so `breaker_state` here records the state at the last persisted attempt and can lag the live breaker. The health API (7.2) returns the live Redis state with `breaker_state_source`, and returns the projection as a nested `projection` object (D26).

---

## 4. Redis keys (ephemeral)

| Key | Type | Purpose |
|---|---|---|
| `rl:ingest:{app_id}` | token bucket (Lua) | ingest rate limit |
| `rl:ep:{endpoint_id}` | token bucket (Lua) | per-endpoint send rate |
| `cc:ep:{endpoint_id}` | counter with TTL | per-endpoint concurrency cap |
| `cb:{endpoint_id}` | hash: `state, failures, open_until, cooldown_ms` | circuit breaker |
| `cb:probe:{endpoint_id}` | string, `SET NX EX 30` | single half-open probe |

All scripts are atomic Lua. If Redis is unavailable the services fail open (D15).

---

## 5. Relationships

```text
EVENT DB      users 1─N apps 1─N api_keys
              apps 1─N events        events/apps 1─N outbox (by aggregate_id)

DELIVERY DB   endpoints 1─N endpoint_subscriptions
              endpoints 1─N deliveries N─1 event_copy
              deliveries 1─N delivery_attempts
              deliveries 1─N delivery_status_history
              deliveries 0..1─ replay_of ─ deliveries
              endpoints 1─1 endpoint_health        apps_ref (projection)

CROSS-SERVICE (IDs only)   events.id → event_copy.event_id → deliveries.event_id
                           apps.id → endpoints.app_id / apps_ref.app_id
```

---

## 6. RabbitMQ topology and messages

### 6.1 Topology (declared by the services at startup, idempotent)

| Exchange | Type | Bindings |
|---|---|---|
| `er.events` | direct | `event.accepted` → `events.in`; `app.upserted` → `apps.sync` |
| `er.delivery` | direct | `deliver` → `delivery.main`; `retry.10s|1m|10m|1h` → matching retry queues; `dead` → `delivery.dlq` |
| `er.dlx` | direct | `poison` → `dlq.poison` |

| Queue | Settings |
|---|---|
| `events.in`, `apps.sync`, `delivery.main` | durable; `x-dead-letter-exchange=er.dlx`, `x-dead-letter-routing-key=poison` |
| `delivery.retry.10s` / `.1m` / `.10m` / `.1h` | durable; fixed `x-message-ttl` (10s / 60s / 600s / 3600s); `x-dead-letter-exchange=er.delivery`, `x-dead-letter-routing-key=deliver` |
| `delivery.dlq` | durable; audit copies; `x-message-ttl` 14 days |
| `dlq.poison` | durable; diagnostics; `x-message-ttl` 14 days |

Publishing uses persistent delivery mode, `mandatory=true` and publisher confirms. Queues are classic durable (single-node demo). A startup/integration check verifies every binding, because `reject(requeue=false)` without a working DLX silently discards.

### 6.2 `EventAccepted` (Event Service → `events.in`)
```json
{
  "message_id": "…outbox id…",
  "message_type": "EventAccepted",
  "schema_version": 1,
  "occurred_at": "2026-10-03T12:00:01Z",
  "correlation_id": "corr_789",
  "event": {
    "event_id": "018f6b3e-8a2c-7c10-9d4e-5b2a1f0c9e11", "app_id": "018f6b3d-1f00-7a22-8b10-3c4d5e6f7a88", "type": "user.created",
    "payload": { "user_id": "u_8742" },
    "schema_version": 1, "accepted_at": "2026-10-03T12:00:01Z"
  }
}
```
All IDs in messages are raw UUIDs (no prefix). `message_id` is stable across republishes of the same outbox row. Message size is capped below 512 KB. No credentials.

### 6.3 `AppUpserted` (Event Service → `apps.sync`)
`{message_id, message_type:"AppUpserted", schema_version:1, app:{app_id, owner_user_id, status, version}}`.

### 6.4 `DeliverWebhook` (Delivery Service → `delivery.main`, also re-published to retry tiers)
`{message_id, message_type:"DeliverWebhook", schema_version:1, delivery_id, correlation_id}`.
A wake-up signal only. The worker always loads state from the database. Retry messages use the same type; there is no separate retry contract and no attempt counter in the message.

### 6.5 Dead-letter metadata
`delivery.dlq` messages carry `delivery_id`, `dead_reason`, `total_attempts`, `last_error_code`, `correlation_id`. `dlq.poison` messages keep the original body, headers and the broker's `x-death` info. Neither copies payloads or secrets. **`delivery.dlq` copies are best-effort audit records (D25):** a failed publish is logged and counted, never retried, and never blocks an ACK; the database is the record. `dlq.poison` is a different mechanism: the broker routes rejected messages there through the DLX.

---

## 7. Public API

Prefix `/v1`. All management routes require `Authorization: Bearer <JWT>`; ingest/read routes require an API key. IDs use prefixed forms. List endpoints use cursor pagination (`limit` ≤ 100, `cursor`). Routing between the two services is defined by the table in 7.0.

### 7.0 Routing table (nginx)

Delivery Service owns some paths that sit under prefixes Event Service otherwise owns, so nginx uses **regex locations for the exceptions** (regex locations take precedence over plain prefix locations; verify the final nginx config with the routing test). Rules are evaluated in this order:

| # | Path pattern | Methods | Service |
|---|---|---|---|
| 1 | `^/v1/apps/[^/]+/endpoints(/.*)?$` | `POST`, `GET` | Delivery |
| 2 | `^/v1/apps/[^/]+/health$` | `GET` | Delivery |
| 3 | `^/v1/events/[^/]+/journey$` | `GET` | Delivery |
| 4 | `^/v1/endpoints(/.*)?$`, `^/v1/deliveries(/.*)?$` | all | Delivery |
| 5 | `^/v1/auth/.*$`, `^/v1/api-keys/.*$` | all | Event |
| 6 | `^/v1/apps(/[^/]+)?$`, `^/v1/apps/[^/]+/api-keys$` | all | Event |
| 7 | `^/v1/events(/[^/]+)?$` | all | Event |
| 8 | anything else under `/v1/` | all | `404` at nginx |

A routing test (Phase 27) requests every row, plus the negative cases `/v1/apps/{id}/api-keys` and `/v1/events/{id}`, which must stay on Event Service. Before nginx exists (Phases 01–26), clients call each service on its own port following this table.

### 7.1 Event Service

| Method & path | Auth | Purpose / notes |
|---|---|---|
| `POST /v1/auth/register` | none | Create a `DEVELOPER` user. Rate-limited. |
| `POST /v1/auth/login` | none | Returns a 1h JWT. |
| `GET /v1/auth/me` | JWT | Current user and role. |
| `POST /v1/apps` | JWT | Create an app (owned by caller). Emits `AppUpserted`. |
| `GET /v1/apps`, `GET /v1/apps/{id}` | JWT | Own apps; `ADMIN` sees all. |
| `PATCH /v1/apps/{id}` | JWT | Name, status, rate limit, burst. Emits `AppUpserted`. |
| `POST /v1/apps/{id}/api-keys` | JWT | Create a key; raw key returned once. |
| `GET /v1/apps/{id}/api-keys` | JWT | Metadata only (prefix, scopes, status). |
| `DELETE /v1/api-keys/{id}` | JWT | Revoke. |
| `POST /v1/events` | API key `events:write` | Ingest (Flow A). `202`. |
| `GET /v1/events/{id}` | API key `events:read` or JWT | Event plus a `status`: `accepted` or `published`. |
| `GET /v1/events` | API key `events:read` or JWT | Filters: `type`, `from`, `to`. For JWT callers `app_id` is required. |

`POST /v1/events` request:
```json
{ "type": "user.created", "data": { "user_id": "u_8742" }, "occurred_at": "2026-10-03T12:00:00Z" }
```
Headers: `Idempotency-Key` (optional), `Content-Type: application/json`.
Response `202`: `{ "event_id": "evt_018f6b3e-8a2c-7c10-9d4e-5b2a1f0c9e11", "status": "accepted", "accepted_at": "…" }`; replays add `Idempotent-Replayed: true`.
Errors: `400` malformed JSON, `401`, `403`, `409 IDEMPOTENCY_KEY_REUSED`, `413`, `415`, `422`, `429` (+`Retry-After`), `503`.

### 7.2 Delivery Service

| Method & path | Auth | Purpose / notes |
|---|---|---|
| `POST /v1/apps/{appId}/endpoints` | JWT | Create. Body: `name`, `url`, `event_types[]`, optional limits. Returns the signing secret **once**. |
| `GET /v1/apps/{appId}/endpoints` | JWT | List (no secrets). |
| `GET /v1/endpoints/{id}` | JWT | Detail with subscriptions. |
| `PATCH /v1/endpoints/{id}` | JWT | Name, URL, timeouts, rate limit, concurrency, `max_attempts`. URL re-validated. |
| `PUT /v1/endpoints/{id}/subscriptions` | JWT | Replace the full subscription set. |
| `POST /v1/endpoints/{id}/pause` · `/resume` | JWT | Pause flips status. Resume also makes its `ENDPOINT_PAUSED`-parked deliveries due now and signals them (Flow F4). |
| `DELETE /v1/endpoints/{id}` | JWT | Soft disable. |
| `POST /v1/endpoints/{id}/rotate-secret` | JWT | New secret shown once; old kept 24h. |
| `POST /v1/endpoints/{id}/test` | JWT | Send a `eventrelay.test` event (Flow F3). |
| `GET /v1/deliveries` | JWT | Filters: `app_id`, `event_id`, `endpoint_id`, `status`, `from`, `to`. |
| `GET /v1/deliveries/{id}` | JWT | Delivery with attempts and status history. |
| `GET /v1/deliveries/{id}/attempts` | JWT | Ordered attempt history. |
| `POST /v1/deliveries/{id}/redrive` | JWT | `DEAD_LETTERED → PENDING` (Flow F1). `202`, `409` if ineligible. |
| `POST /v1/deliveries/{id}/replay` | JWT | New linked delivery (Flow F2). `202`, `409`, `410 PAYLOAD_EXPIRED`. **The replay reuses the original `webhook-id`, so a receiver that deduplicates on it will intentionally ignore the replay.** |
| `GET /v1/endpoints/{id}/health` | JWT | `{ "breaker_state": <live from Redis>, "breaker_state_source": "live" or "projection" (projection when Redis is unreachable), "projection": { "breaker_state", "consecutive_failures", "last_success_at", "last_failure_at", "last_http_status", "total_success", "total_failure", "updated_at" } }`. The projection may lag the live breaker. |
| `GET /v1/apps/{id}/health` | JWT | Delivery counts by status, open breakers, oldest pending age. |
| `GET /v1/events/{eventId}/journey` | JWT | Event summary status and its deliveries (Flow Section 11). |

Redrive and replay accept `{ "reason": "…" }` (required, recorded in history with the actor).

### 7.3 Operational (both services)
`GET /actuator/health/liveness` (never depends on downstream), `GET /actuator/health/readiness` (DB, plus broker/Redis checks that reflect the documented fail-open policy), `GET /actuator/prometheus` (internal network only; blocked at nginx).

### 7.4 Error format
```json
{ "error": { "code": "IDEMPOTENCY_KEY_REUSED", "message": "…", "request_id": "req_123" } }
```
Stable `code`; safe `message`; no stack traces, SQL, hostnames or secrets. `Retry-After` on `429` and `503` where applicable.

Codes: `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `PAYLOAD_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE`, `IDEMPOTENCY_KEY_REUSED`, `RATE_LIMITED`, `NOT_FOUND`, `INVALID_STATE`, `PAYLOAD_EXPIRED`, `UNSAFE_DESTINATION`, `INTERNAL`, `UNAVAILABLE`.

---

## 8. Authentication and authorization matrix

| Operation | Credential | Rule |
|---|---|---|
| Ingest / read events | API key | Key `ACTIVE`, unexpired, has scope; app `ACTIVE`; scope is the key's app only |
| Register / login | none | Rate-limited |
| Apps, API keys | JWT | Owner or `ADMIN` |
| Endpoints, deliveries, replay, redrive, test, health | JWT | Owner of the app (via `apps_ref`) or `ADMIN`; cross-tenant objects return `404` |
| Actuator prometheus | internal network | Blocked at nginx |

Never trust an `app_id` from a request body to prove ownership. For ingest, the app comes from the API key. For management, ownership is checked at the repository/service layer, not only in controllers. JWT claims: `sub` (user id), `role`, `iss`, `exp`. Event Service signs with its private key; Delivery Service verifies with the configured public key.

---

## 9. Table-to-flow map

| Flow action | Service / table |
|---|---|
| Register, login | Event: `users` |
| Create/change app | Event: `apps` + `outbox(AppUpserted)` → Delivery: `apps_ref` |
| Create/revoke API key | Event: `api_keys` |
| Accept event; dedupe repeats | Event: `events` (+ unique idempotency index) |
| Survive crashes between DB and broker | Event: `outbox` |
| Register endpoint, subscribe | Delivery: `endpoints`, `endpoint_subscriptions` |
| Store event for delivery | Delivery: `event_copy` |
| Fan-out, prevent duplicates | Delivery: `deliveries` (unique partial index) |
| Record each HTTP attempt | Delivery: `delivery_attempts` |
| Audit status changes, redrive/replay actor | Delivery: `delivery_status_history` |
| Health read model | Delivery: `endpoint_health` |
| Rate limits, concurrency, breaker | Redis keys (Section 4) |
| Delayed retry | RabbitMQ tier queues + `deliveries.next_retry_at` |
| Inspect dead letters | `deliveries` (+ `delivery.dlq` audit copy, `dlq.poison` for malformed messages) |

---

## 10. Contract principles

1. Every table has one owning service; no cross-database foreign keys.
2. Public IDs are stable across retries and duplicates.
3. Database constraints enforce uniqueness under concurrency.
4. Broker messages are versioned; consumers tolerate duplicates.
5. RabbitMQ and Redis are never the source of truth for business state.
6. Attempt history is append-only; redrive and replay never erase it.
7. API responses distinguish "accepted" from "delivered".
8. Secrets are hashed or encrypted and shown once.
9. A table or column exists only because a flow step uses it.
