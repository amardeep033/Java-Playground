# Tables · PostgreSQL schema `service_a`

Database: `postgres` on `localhost:5432`; username/password: `postgres`/`postgres`.
SQL: `src/main/resources/db/schema.sql` (runs on startup).

| Table | Columns | Constraints |
| --- | --- | --- |
| users | id, name, email, password_hash, created_at, deleted_at? | PK id; unique lowercase email |
| applications | id, user_id, name, created_at, deleted_at? | FK user_id → users; unique (user_id, name) |
| event_types | id, application_id, name, description?, created_at, deleted_at? | FK application_id → applications; unique (application_id, name) |
| endpoints | id, application_id, url, description?, created_at, deleted_at?, enabled, rate_limit_per_second, concurrency_limit, timeout_ms, max_attempts, signing_secret_encrypted?, signing_secret_version, signing_secret_rotated_at? | FK application_id → applications; unique (application_id, url) |
| endpoint_event_types | endpoint_id, event_type_id | composite PK; FKs → endpoints/event_types |
| api_keys | id, application_id, name, key_hash, key_prefix, created_at, revoked_at?, replaced_by_id?, deleted_at? | FK application_id → applications; FK replaced_by_id → api_keys; unique key_hash |

- IDs: UUID. created_at: timestamptz. `?`: nullable.
- name: varchar(120); email: varchar(320); description: varchar(500); url: varchar(2048).
- password_hash: varchar(255), PBKDF2-SHA256 with random 16-byte salt and 310,000 iterations. Never plaintext.
- key_hash: varchar(64), SHA-256 of a random 32-byte API secret; key_prefix: varchar(16), display only.
- API-key rotation: old row revoked_at + replaced_by_id; new row has a new hash. Row lock serializes rotations.
- password_hash and key_hash are absent from all API responses.
- One user → many apps; one app → many event types/endpoints.
- Endpoints ↔ event types: many-to-many; same-app rule enforced by service.
- No physical cascading deletes. FK lookups covered by unique indexes; extra index on subscription event_type_id.

## Sample data

Illustrative SQL for the six registration tables. Event ingestion fills the messaging tables below. Run manually after starting the service if you want seed data. Curl tests create separate rows with generated IDs.

```sql
BEGIN;

INSERT INTO service_a.users (id, name, email, password_hash)
VALUES ('11111111-1111-1111-1111-111111111111', 'Alice', 'alice@example.com',
        '00112233445566778899aabbccddeeff3b36532e181e2b7f69142854218ac2855c5a077b8cd8a15e44c1686f52e795e7');

INSERT INTO service_a.applications (id, user_id, name)
VALUES ('22222222-2222-2222-2222-222222222222',
        '11111111-1111-1111-1111-111111111111', 'Order App');

INSERT INTO service_a.event_types (id, application_id, name, description)
VALUES ('33333333-3333-3333-3333-333333333333',
        '22222222-2222-2222-2222-222222222222',
        'order.created', 'A new order was created');

INSERT INTO service_a.endpoints (id, application_id, url, description)
VALUES ('44444444-4444-4444-4444-444444444444',
        '22222222-2222-2222-2222-222222222222',
        'https://example.com/webhooks/orders', 'Order updates');

INSERT INTO service_a.endpoint_event_types (endpoint_id, event_type_id)
VALUES ('44444444-4444-4444-4444-444444444444',
        '33333333-3333-3333-3333-333333333333');

-- History example only: this API key is already revoked and cannot be used.
INSERT INTO service_a.api_keys (id, application_id, name, key_hash, key_prefix, revoked_at)
VALUES ('55555555-5555-5555-5555-555555555555',
        '22222222-2222-2222-2222-222222222222', 'Old development key',
        'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
        'er_example', now());

COMMIT;
```

`created_at` defaults to the insertion time. Run this seed once; repeated inserts return duplicate errors.

Sample user password: `ExamplePass123!`. The fixed salt above is for demo seed data; API registration generates a fresh random salt.

## Lifecycle and endpoint configuration

- `deleted_at`: nullable timestamptz on users, applications, event_types, endpoints, api_keys. Active rows have NULL. ORM reads filter deleted rows; ORM delete operations update timestamps.
- User/app deletion marks descendants in the same transaction; subscription rows remain. API keys are also revoked. Active reads cannot access deleted parents or their children.
- Endpoint configuration: enabled boolean=true; rate_limit_per_second int=10; concurrency_limit int=4; timeout_ms int=10000; max_attempts int=5.
- Signing fields: signing_secret_encrypted text (AES-256-GCM nonce+ciphertext+tag); signing_secret_version int=0; signing_secret_rotated_at timestamptz. New endpoint APIs generate version 1; old/seed endpoints require secret rotation to initialize one.
- Signing ciphertext is recoverable only with the master key; API-key hashes remain one-way. Secrets/ciphertext are never returned by metadata APIs.
- Local master key: `service-a/.local/signing.key` when launched with `run1.sh`; created with owner-only file permissions and ignored by Git. Keep it across restarts; losing it makes stored signing secrets unreadable.
- Override with `SIGNING_SECRET_ENCRYPTION_KEY` (Base64, 32 bytes), or `SIGNING_SECRET_KEY_FILE` (persistent path). Use the same master key wherever the future delivery service must decrypt secrets.
- Existing unique names/emails/URLs stay reserved after deletion. No physical cascading deletes.

## Event ingestion and delivery projections

| Table | Purpose / key fields |
| --- | --- |
| received_events | id PK, application_id FK, event_type_id FK, event_type snapshot, idempotency_key, request_hash, payload JSONB, delivery_count, created_at, deleted_at; unique (application_id, idempotency_key) |
| delivery_projections | id PK, event_id FK, application_id FK, endpoint_id FK, status, attempt_count, last_sequence, http_status, last_error, next_attempt_at, timestamps, deleted_at; unique (event_id, endpoint_id) |
| status_inbox | message_id PK, delivery_id FK, processed_at |
| outbox_messages | id PK, aggregate_id, exchange_name, routing_key, payload JSONB, claim_token, locked_until, available_at, attempt_count, published_at, last_error, timestamps, deleted_at |

- Event, initial projection and outgoing message commit in one transaction. API idempotency is scoped to application + key; canonical JSON object ordering avoids false conflicts.
- Configuration/secret snapshots live in outbox messages. Disabled/deleted endpoints are excluded only when accepting a new event.
- Parent deletion leaves event/delivery history intact; normal event APIs reject deleted parents. Already queued deliveries and their status updates continue.
- Outbox messages are marked published, never physically removed. Status inbox deduplicates updates; last_sequence rejects stale updates.

## Monitoring and manual replay

| Table / change | Fields and rules |
| --- | --- |
| delivery_attempt_projections | (delivery_id, attempt_number) PK; HTTP status, error, started_at, completed_at, response_body?, content_type?, response_truncated; populated from B status messages |
| replay_requests | (application_id, idempotency_key) PK; source_delivery_id UNIQUE FK; delivery_id UNIQUE FK; created_at |
| delivery_projections additions | endpoint_url snapshot; replay_of_id nullable self FK; unique original (event_id, endpoint_id) when replay_of_id IS NULL; unique replay_of_id when non-null |

- Replay creates a linked delivery and outbox job atomically. Old failures and attempts remain unchanged.
- Per-delivery sequence protects current status, while late attempt messages still fill missing history.
- Activity APIs read only Service A's schema; no cross-service DB reads.

## Authentication

| Table / column | Purpose | Sample |
| --- | --- | --- |
| users.role | USER (default), ADMIN | USER |
| auth_sessions | Revocable JWT sessions; id, user_id, created_at, expires_at, revoked_at, ip_address | expires_at=2026-11-05T16:00:00Z, revoked_at=null |
| login_guards | SHA-256 email_hash, failures, window_started_at, locked_until | failures=5, locked_until=2026-11-05T08:15:00Z |
| login_attempts | id, user_id (nullable), email_hash, attempted_at, success, reason, ip_address | success=false, reason=BAD_CREDENTIALS, ip_address=127.0.0.1 |

Reasons: SUCCESS, BAD_CREDENTIALS, LOCKED. No plaintext passwords, JWTs or API keys. Sessions are revoked; audit rows are retained.

Administrator bootstrap (trusted DB access only; use your registered email):

```sql
UPDATE service_a.users SET role='ADMIN' WHERE email='your-email@example.com' AND deleted_at IS NULL;
```

No role field is accepted by public registration. Role changes take effect on the next request.
