# Schema `service_b`

| Table | Purpose / key fields |
| --- | --- |
| deliveries | id PK, event_id, application_id, endpoint_id, immutable command snapshot JSONB, status, attempt_count, status_sequence, http_status, last_error, next_attempt_at, timestamps, deleted_at |
| delivery_attempts | id PK, delivery_id FK, attempt_number, http_status, error, started_at, completed_at; unique (delivery_id, attempt_number) |
| delivery_inbox | message_id PK, delivery_id FK, processed_at |
| outbox_messages | id PK, aggregate_id, exchange_name, routing_key, payload JSONB, available_at, claim_token, locked_until, attempt_count, published_at, last_error, timestamps, deleted_at |

- UUID IDs; timestamptz timestamps. No cross-service foreign keys.
- Unique delivery `(event_id, endpoint_id)`; per-delivery row lock serializes processing.
- Command contains the encrypted signing secret, not plaintext. Secret remains encrypted in DB and broker messages.
- Attempt/inbox rows are append-only history. Outbox rows are marked published; rows are not physically deleted.
- SQL: `src/main/resources/db/schema.sql`.

- Manual replay: deliveries.replay_of_id links the source delivery ID. Original (event_id, endpoint_id) stays unique only for non-replay jobs; each source may have one child replay.
- Attempts now include response_body, content_type, response_truncated and monitoring_exported_at. Pre-upgrade attempts are exported through the outbox without sending another webhook.
- Response previews are limited to 4096 bytes. The full response and authorization/cookie headers are not stored.
