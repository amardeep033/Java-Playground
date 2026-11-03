# Service B API

Internal/local inspection only. Events enter through Service A, not this service.

| Method | Path | Curl |
| --- | --- | --- |
| GET | /internal/v1/deliveries/{deliveryId} | `curl -sS 'http://localhost:8082/internal/v1/deliveries/REPLACE_DELIVERY_ID' \| jq` |

Response: delivery/event/app/endpoint IDs, status, attempt_count, http_status, last_error, next_attempt_at, timestamps, replay_of_id and attempts[] (including bounded response previews). No signing secret, ciphertext or webhook payload.

Signed webhook headers:

| Header | Value |
| --- | --- |
| Content-Type | application/json |
| webhook-id | delivery UUID; stable across retries |
| webhook-timestamp | Unix seconds for this attempt |
| webhook-signature | v1,BASE64_HMAC_SHA256 |
| Idempotency-Key | delivery UUID |
| X-Event-Relay-Event-Id | ingested event UUID |
| X-Event-Relay-Event-Type | registered event type name |
| X-Event-Relay-Attempt | 1-based attempt number |

Signature input: `deliveryId.timestamp.rawBody`. HMAC key: Base64URL-decode the signing secret after `whsec_`. Sign the exact UTF-8 body bytes; compare signatures securely and reject old timestamps in your receiver.
