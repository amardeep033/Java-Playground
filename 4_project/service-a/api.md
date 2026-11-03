# Service A APIs

Run: `mvn spring-boot:run` from `service-a` (Java 21). Requires Bash, curl and jq.

Run setup, then use the table. Registration signs in automatically. Cookie + CSRF protect management APIs. API-key event clients remain supported.

```bash
BASE_URL='http://localhost:8081/api/v1'
RUN_ID="$(date +%s)-$RANDOM"
PASSWORD='ExamplePass123!'
COOKIE_JAR=$(mktemp)
curl --fail-with-body -sS -c "$COOKIE_JAR" "$BASE_URL/auth/csrf" >/dev/null
```

| Method | Path | Curl | Status |
| --- | --- | --- | --- |
| POST | /api/v1/users | `USER_ID=$(curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X POST "$BASE_URL/users"  -H 'Content-Type: application/json'  -d "{\"name\":\"Alice\",\"email\":\"alice+$RUN_ID@example.com\",\"password\":\"$PASSWORD\"}" \| jq -er '.id')` | 201 |
| GET | /api/v1/users/{userId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/users/$USER_ID" \| jq` | 200 |
| POST | /api/v1/users/{userId}/applications | `APP_ID=$(curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X POST "$BASE_URL/users/$USER_ID/applications"  -H 'Content-Type: application/json'  -d '{"name":"Order App"}' \| jq -er '.id')` | 201 |
| GET | /api/v1/users/{userId}/applications | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/users/$USER_ID/applications?page=0&size=20" \| jq` | 200 |
| GET | /api/v1/users/{userId}/applications/{applicationId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/users/$USER_ID/applications/$APP_ID" \| jq` | 200 |
| POST | /api/v1/applications/{applicationId}/event-types | `EVENT_TYPE_ID=$(curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X POST "$BASE_URL/applications/$APP_ID/event-types"  -H 'Content-Type: application/json'  -d '{"name":"order.created","description":"A new order was created"}' \| jq -er '.id')` | 201 |
| GET | /api/v1/applications/{applicationId}/event-types | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/applications/$APP_ID/event-types?page=0&size=20" \| jq` | 200 |
| GET | /api/v1/applications/{applicationId}/event-types/{eventTypeId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/applications/$APP_ID/event-types/$EVENT_TYPE_ID" \| jq` | 200 |
| POST | /api/v1/applications/{applicationId}/endpoints | `ENDPOINT_RESPONSE=$(curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X POST "$BASE_URL/applications/$APP_ID/endpoints"  -H 'Content-Type: application/json'  -d "{\"url\":\"https://example.com/webhooks/orders\",\"description\":\"Order updates\",\"eventTypeIds\":[\"$EVENT_TYPE_ID\"]}"); ENDPOINT_ID=$(jq -er '.endpoint.id' <<< "$ENDPOINT_RESPONSE"); SIGNING_SECRET=$(jq -er '.signingSecret' <<< "$ENDPOINT_RESPONSE"); echo "$ENDPOINT_RESPONSE" \| jq` | 201 |
| GET | /api/v1/applications/{applicationId}/endpoints | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/applications/$APP_ID/endpoints?page=0&size=20" \| jq` | 200 |
| GET | /api/v1/applications/{applicationId}/endpoints/{endpointId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/applications/$APP_ID/endpoints/$ENDPOINT_ID" \| jq` | 200 |
| POST | /api/v1/applications/{applicationId}/api-keys | `KEY_RESPONSE=$(curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X POST "$BASE_URL/applications/$APP_ID/api-keys" -H 'Content-Type: application/json' -d '{"name":"Development"}'); KEY_ID=$(jq -er '.key.id' <<< "$KEY_RESPONSE"); API_KEY=$(jq -er '.secret' <<< "$KEY_RESPONSE"); echo "$KEY_RESPONSE" \| jq` | 201 |
| GET | /api/v1/applications/{applicationId}/api-keys | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/applications/$APP_ID/api-keys?page=0&size=20" \| jq` | 200 |
| GET | /api/v1/applications/{applicationId}/api-keys/{keyId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" --fail-with-body -sS "$BASE_URL/applications/$APP_ID/api-keys/$KEY_ID" \| jq` | 200 |
| POST | /api/v1/applications/{applicationId}/api-keys/{keyId}/rotate | `ROTATED_RESPONSE=$(curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X POST "$BASE_URL/applications/$APP_ID/api-keys/$KEY_ID/rotate"); NEW_KEY_ID=$(jq -er '.key.id' <<< "$ROTATED_RESPONSE"); API_KEY=$(jq -er '.secret' <<< "$ROTATED_RESPONSE"); echo "$ROTATED_RESPONSE" \| jq` | 201 |
| DELETE | /api/v1/applications/{applicationId}/api-keys/{keyId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X DELETE "$BASE_URL/applications/$APP_ID/api-keys/$NEW_KEY_ID"` | 204 |
| PUT | /api/v1/applications/{applicationId}/endpoints/{endpointId}/configuration | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X PUT "$BASE_URL/applications/$APP_ID/endpoints/$ENDPOINT_ID/configuration" -H 'Content-Type: application/json' -d '{"enabled":true,"rateLimitPerSecond":25,"concurrencyLimit":8,"timeoutMs":15000,"maxAttempts":5}' \| jq` | 200 |
| POST | /api/v1/applications/{applicationId}/endpoints/{endpointId}/signing-secret/rotate | `SIGNING_RESPONSE=$(curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" --fail-with-body -sS -X POST "$BASE_URL/applications/$APP_ID/endpoints/$ENDPOINT_ID/signing-secret/rotate"); SIGNING_SECRET=$(jq -er '.signingSecret' <<< "$SIGNING_RESPONSE"); echo "$SIGNING_RESPONSE" \| jq` | 201 |

- POST: JSON → 201 + saved resource (id, fields, createdAt). GET: 200.
- Lists: `?page=0&size=20` (max 100); response: items, page, size, totalElements, totalPages.
- Errors: 400 invalid input/cross-app subscription; 401 wrong current password; 404 missing resource; 409 duplicate/revoked-key rotation.
- Event types are registered definitions, e.g. `order.created`; allowed name characters: letters, digits, `_ . -`.
- Endpoint: HTTP(S) URL; at least one event type from the same app.
- Passwords: 12–128 characters; salted PBKDF2-SHA256 hashes only; never included in responses. Change requires current password; reusing it returns 400.
- API-key create/rotate returns `{key: {id, applicationId, name, prefix, createdAt, revokedAt, replacedById}, secret}` with `Cache-Control: no-store`. Save `secret` immediately; GET/list never return it or its hash.
- Rotation immediately revokes the old key and records its replacement in one transaction. Revocation is repeatable (204); revoked keys remain in lists for history.
- Keys use 32 random bytes; DB stores SHA-256 hashes. Future ingestion header: `X-API-Key`.
- Dashboard: cookie JWT + CSRF + ownership/RBAC. Server-to-server events: X-API-Key. See authentication below.

## Error checks

| Case | Curl | Status |
| --- | --- | --- |
| Duplicate event type | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X POST "$BASE_URL/applications/$APP_ID/event-types"  -H 'Content-Type: application/json'  -d '{"name":"order.created"}'` | 409 |
| Invalid user | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X POST "$BASE_URL/users"  -H 'Content-Type: application/json'  -d '{"name":"","email":"invalid"}'` | 400 |
| Another user ID | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -i "$BASE_URL/users/00000000-0000-0000-0000-000000000000"` | 403 |
| Short password | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X POST "$BASE_URL/users" -H 'Content-Type: application/json' -d '{"name":"Alice","email":"short@example.com","password":"short"}'` | 400 |
| Rotate old/revoked key | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X POST "$BASE_URL/applications/$APP_ID/api-keys/$KEY_ID/rotate"` | 409 |
| Wrong current password | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X PUT "$BASE_URL/users/$USER_ID/password" -H 'Content-Type: application/json' -d '{"currentPassword":"IncorrectPass!","newPassword":"AnotherPass123!"}'` | 401 |

## Send a webhook through RabbitMQ

Start RabbitMQ and both services using `../service_b/README.md`. Use an enabled endpoint subscribed to the event type, with a configured signing secret. Use an **active** application API key; the earlier rotation/revocation examples invalidate their old keys.

```bash
BASE_URL='http://localhost:8081/api/v1'
APP_ID='REPLACE_WITH_YOUR_APPLICATION_ID'
read -r -s -p 'Active application API key: ' API_KEY; echo
SEND_KEY="webhook-test-$(date +%s)-$RANDOM"
```

| Method | Path | Curl | Status |
| --- | --- | --- | --- |
| POST | /api/v1/applications/{applicationId}/events | `EVENT_ID=$(curl --fail-with-body -sS -X POST "$BASE_URL/applications/$APP_ID/events" -H "X-API-Key: $API_KEY" -H "Idempotency-Key: $SEND_KEY" -H 'Content-Type: application/json' -d '{"eventType":"order.created","payload":{"orderId":"test-001","amount":500}}' \| jq -er '.id'); echo "$EVENT_ID"` | 202 |
| GET | /api/v1/applications/{applicationId}/events/{eventId} | `curl --fail-with-body -sS "$BASE_URL/applications/$APP_ID/events/$EVENT_ID" -H "X-API-Key: $API_KEY" \| jq` | 200 |

- Use the exact registered event type name. POST returns id, eventType, deliveryCount, createdAt; delivery is asynchronous. No subscribers → deliveryCount=0.
- Repeat the same POST with the same `SEND_KEY` to get the same receipt without creating more deliveries. Changed event/body with that key → 409. Use a fresh key for a new event. Payload max: 1 MB.
- Missing/invalid/revoked API key → 401; another app’s key → 403; unknown event type/event → 404; missing idempotency key → 400.
- GET returns payload, overall status and deliveries[] (id, endpointId, status, attemptCount, httpStatus, error, nextAttemptAt, updatedAt).
- Overall: PENDING / SUCCEEDED / FAILED / NO_SUBSCRIBERS. Delivery: PENDING / RETRY_SCHEDULED / SUCCEEDED / DEAD.
- Watch your webhook.site page for the POST. The webhook body contains id, type, createdAt and data. Signatures and retry policy: `../service_b/api.md` and `../service_b/rabbitmq.md`.
- Timeout/max attempts are enforced; rate/concurrency limits are per B instance until the planned shared Redis limiter.

## Monitoring and replay

All calls require `X-API-Key` for the selected app. `page` starts at 0; `size` is 1–100. Lists return items, page, size, totalElements, totalPages.

```bash
DELIVERY_ID='REPLACE_WITH_A_FAILED_DELIVERY_ID'
REPLAY_KEY="manual-replay-$(date +%s)-$RANDOM"
```

| Method | Path | Curl | Status |
| --- | --- | --- | --- |
| GET | /api/v1/applications/{applicationId}/events | `curl -sS "$BASE_URL/applications/$APP_ID/events?page=0&size=20" -H "X-API-Key: $API_KEY" \| jq` | 200 |
| GET | /api/v1/applications/{applicationId}/activity-summary | `curl -sS "$BASE_URL/applications/$APP_ID/activity-summary" -H "X-API-Key: $API_KEY" \| jq` | 200 |
| GET | /api/v1/applications/{applicationId}/deliveries | `curl -sS "$BASE_URL/applications/$APP_ID/deliveries?status=DEAD&includeReplayed=false&page=0&size=20" -H "X-API-Key: $API_KEY" \| jq` | 200 |
| GET | /api/v1/applications/{applicationId}/deliveries/{deliveryId} | `curl -sS "$BASE_URL/applications/$APP_ID/deliveries/$DELIVERY_ID" -H "X-API-Key: $API_KEY" \| jq` | 200 |
| POST | /api/v1/applications/{applicationId}/deliveries/{deliveryId}/replay | `REPLAY_ID=$(curl --fail-with-body -sS -X POST "$BASE_URL/applications/$APP_ID/deliveries/$DELIVERY_ID/replay" -H "X-API-Key: $API_KEY" -H "Idempotency-Key: $REPLAY_KEY" \| jq -er '.id'); echo "$REPLAY_ID"` | 202 |

- List filters: `q` (ID/type; deliveries also search destination), `status`; deliveries additionally accept `eventId`, `endpointId`, `includeReplayed`.
- Event statuses: PENDING, SUCCEEDED, FAILED, NO_SUBSCRIBERS. Delivery statuses: PENDING, RETRY_SCHEDULED, SUCCEEDED, DEAD.
- Event detail accepts `includeDeliveries=false` for metadata/payload only; fetch its deliveries with the paginated `eventId` filter.
- Delivery detail: delivery metadata, original requestBody, attempts[] (number, HTTP status, error, start/end, durationMs, responseBody, contentType, responseTruncated).
- Text/JSON/XML response previews are limited to 4096 bytes; binary previews and sensitive response headers are not stored. Older attempts synchronize through B's outbox; their originally uncaptured response bodies remain unavailable.
- Replay only DEAD deliveries with a live/enabled endpoint, configured signing secret and active event type. Ineligible replay → 409 with a reason.
- One child replay per failed delivery. Reusing its idempotency key returns the same receipt; another key for an already replayed source → 409. A failed replay can itself be replayed.
- Replay returns a new id, original eventId, replayOfId and createdAt. It preserves the original event body and uses current endpoint settings/secret. Original attempts and status remain in history.
- Event overall status follows the latest delivery in each replay chain. `failed` in activity-summary counts failed deliveries without a replay; deliveries/succeeded/replays counts retain history.
- Dead letters in the UI are persisted failed-delivery records. Original RabbitMQ DLQ messages remain transport history; replay creates a fresh job. Invalid broker messages without a delivery record remain visible in RabbitMQ management.

## Deletion — run last

| Method | Path | Curl | Status |
| --- | --- | --- | --- |
| DELETE | /api/v1/applications/{applicationId}/endpoints/{endpointId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X DELETE "$BASE_URL/applications/$APP_ID/endpoints/$ENDPOINT_ID"` | 204 |
| DELETE | /api/v1/applications/{applicationId}/event-types/{eventTypeId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X DELETE "$BASE_URL/applications/$APP_ID/event-types/$EVENT_TYPE_ID"` | 204 |
| DELETE | /api/v1/users/{userId}/applications/{applicationId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X DELETE "$BASE_URL/users/$USER_ID/applications/$APP_ID"` | 204 |
| DELETE | /api/v1/users/{userId} | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X DELETE "$BASE_URL/users/$USER_ID"` | 204 |

- Always soft-delete: rows are retained; deleted resources disappear from GET/list and return 404. Repeat delete returns 404. Names/emails/URLs remain reserved by existing unique constraints.
- User deletion cascades to applications; application deletion cascades to endpoints, event types and API keys. Subscription rows stay for history; deleted event types are filtered from active endpoint subscriptions.
- API-key DELETE continues to revoke (`revoked_at`) and retains visible history. Parent deletion also sets key `deleted_at` and revokes it.
- Endpoint creation now returns `{endpoint: {...}, signingSecret}`. Signing-secret rotation returns the same envelope; both use `Cache-Control: no-store`. GET/list never reveal secrets or ciphertext.
- Signing secrets: generated `whsec_...`; encrypted with AES-256-GCM, bound to endpoint ID. Rotation replaces the old secret immediately; no overlap window.
- Endpoint configuration defaults: enabled=true, rateLimitPerSecond=10, concurrencyLimit=4, timeoutMs=10000, maxAttempts=5. Optional `configuration` can also be supplied during creation.
- Configuration bounds: rate 1–100000/s, concurrency 1–1000, timeout 100–60000ms, attempts 1–20. Service B applies these snapshots to new deliveries; local limits are per instance.

## Authentication

- Existing users log in with their registered email/password; new accounts default to USER.
- ER_SESSION: HttpOnly, SameSite=Lax, 8 hours, server-revocable JWT. No JWT/API key in browser storage.
- USER: own workspace/apps. ADMIN: all workspaces/apps + user directory. Changing another user's password is forbidden.
- Five failed passwords within 15 minutes lock that email for 15 minutes. Correct login resets failures. Unknown emails follow the same policy.
- Attempts: last 20 for the signed-in account; timestamp, result, IP. No passwords/tokens in audit.
- Password change signs out every session. API keys remain independent.

Use BASE_URL and COOKIE_JAR from setup above. CSRF cookie is rotated after login/registration.

| Method | Path | Curl | Status |
| --- | --- | --- | --- |
| GET | /auth/csrf | `curl -sS -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$BASE_URL/auth/csrf"` | 200 |
| POST | /auth/login | `curl -i -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -H 'Content-Type: application/json' -d "{\"email\":\"alice+$RUN_ID@example.com\",\"password\":\"$PASSWORD\"}" "$BASE_URL/auth/login"` | 200 / 401 / 429 |
| GET | /auth/me | `curl -sS -b "$COOKIE_JAR" "$BASE_URL/auth/me"` | 200 / 401 |
| GET | /auth/login-attempts | `curl -sS -b "$COOKIE_JAR" "$BASE_URL/auth/login-attempts"` | 200 |
| GET | /auth/admin/users | `curl -sS -b "$COOKIE_JAR" "$BASE_URL/auth/admin/users"` | 200 ADMIN / 403 USER |
| POST | /auth/logout | `curl -i -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -X POST "$BASE_URL/auth/logout"` | 204 |
| POST | /auth/logout-all | `curl -i -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -X POST "$BASE_URL/auth/logout-all"` | 204 |

Password change (while signed in; log in again afterward with the new password):

| Method | Path | Curl | Status |
| --- | --- | --- | --- |
| PUT | /api/v1/users/{userId}/password | `curl -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H "X-XSRF-TOKEN: $(awk '$6=="XSRF-TOKEN" {print $7}' "$COOKIE_JAR")" -i -X PUT "$BASE_URL/users/$USER_ID/password" -H 'Content-Type: application/json' -d "{\"currentPassword\":\"$PASSWORD\",\"newPassword\":\"ChangedPass123!\"}"` | 204 |
