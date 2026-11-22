Delivery/attempt tables are read projections. B owns delivery state; only final completion events or cursor-based REST reconciliation populate outcomes and attempt history.

Applications combine metadata and activation in one PUT; omitted fields remain unchanged and null clears description. Event types keep separate metadata/activation APIs. Endpoints have common name/config updates and ADMIN-only activation/URL/mapping updates. Activation remains ADMIN-only, audited and unversioned.

Users & access reads app options and each user’s assigned application IDs from the company user-list API. Mapping changes still update workspace_members; no schema change.

Login-attempt success is derived from `reason = SUCCESS`; it is not stored separately.

Roles live only in `users.role`: ADMIN manages one company and every company app; USER accesses mapped apps. Public signup creates a company and its first ADMIN. Company admins create other accounts. Deletes are soft; keep at least one ADMIN.

| table_number | column_number | table_name | column_name | constraint | usage |
| --- | --- | --- | --- | --- | --- |
| 1 | 1.1 | api_keys | id | uuid, not_nullable, pk | API-key ID; example: 55555555-5555-5555-5555-555555555555; Can't update |
| 1 | 1.2 | api_keys | application_id | uuid, not_nullable, fk applications(id) | Application this key can access; Can't update |
| 1 | 1.3 | api_keys | name | character varying(120), not_nullable | Editable key label; example: Development key; In place; any app member; audit |
| 1 | 1.4 | api_keys | key_hash | character varying(64), not_nullable, unique | SHA-256 of random API-key secret; used for authentication; never returned; Can't update |
| 1 | 1.5 | api_keys | key_prefix | character varying(16), not_nullable | Display-only prefix; example: er_example; Can't update |
| 1 | 1.6 | api_keys | created_at | timestamp with time zone, not_nullable, default now() | Key creation time; Can't update |
| 1 | 1.7 | api_keys | revoked_at | timestamp with time zone | Revocation time; non-NULL blocks key authentication; System-managed; can't update directly |
| 1 | 1.8 | api_keys | replaced_by_id | uuid, fk api_keys(id) | New key created by rotation; NULL for direct revocation or an unrotated key; System-managed; can't update directly |
| 1 | 1.9 | api_keys | deleted_at | timestamp with time zone | Soft-delete time; deleted keys are also revoked; System-managed; can't update directly |
| 2 | 2.1 | applications | id | uuid, not_nullable, pk | Application ID; example: 22222222-2222-2222-2222-222222222222; Can't update |
| 2 | 2.2 | applications | user_id | uuid, not_nullable, fk users(id) | Creating user; client owns the app; Can't update |
| 2 | 2.3 | applications | name | character varying(120), not_nullable | Editable application name; example: Order App; reserved after deletion; In place; ADMIN; audit |
| 2 | 2.4 | applications | created_at | timestamp with time zone, not_nullable, default now() | Application creation time; Can't update |
| 2 | 2.5 | applications | deleted_at | timestamp with time zone | Soft-delete time; configuration hidden, historical rows retained; System-managed; can't update directly |
| 2 | 2.6 | applications | is_active | boolean, not_nullable, default true | Temporary pause; false blocks new events/replays; history remains readable; In place; ADMIN; audit |
| 2 | 2.7 | applications | description | character varying(500) | Editable description; example: Order processing webhooks; In place; ADMIN; no audit |
| 2 | 2.8 | applications | client_id | uuid, not_nullable, fk clients(id) | Owning client; example: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa; Can't update |
| 3 | 3.1 | audit_logs | id | uuid, not_nullable, pk | Change audit ID; Can't update |
| 3 | 3.2 | audit_logs | application_id | uuid, fk applications(id) | Workspace scope; NULL for company account/password changes; Can't update |
| 3 | 3.3 | audit_logs | actor_id | uuid, not_nullable, fk users(id) | Authenticated actor ID; Can't update |
| 3 | 3.4 | audit_logs | entity_type | character varying(40), not_nullable | Changed resource; example: endpoints; audit UI groups users/workspace_members as User; Can't update |
| 3 | 3.5 | audit_logs | entity_id | uuid, not_nullable | Changed resource ID; logical reference; Can't update |
| 3 | 3.6 | audit_logs | action | character varying(64), not_nullable | Stored action; examples: UPDATE, CONFIG_UPDATED, PASSWORD_CHANGED; admin audit API groups as create/update/delete; Can't update |
| 3 | 3.7 | audit_logs | details | jsonb, not_nullable, default '{}'::jsonb | Safe old/new metadata and configuration; example: {"field":"name","old":"Orders","new":"Order webhook"}; no passwords, hashes, API keys or signing credentials; Can't update |
| 3 | 3.8 | audit_logs | created_at | timestamp with time zone, not_nullable, default now() | Successful change commit time; append-only through APIs; Can't update |
| 3 | 3.9 | audit_logs | client_id | uuid, not_nullable, fk clients(id) | Required company scope for every resource/account change; Can't update |
| 4 | 4.1 | clients | id | uuid, not_nullable, pk | Client ID; example: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa; Can't update |
| 4 | 4.2 | clients | name | character varying(120), not_nullable | Company/organization name; example: Acme; Can't update through the API |
| 4 | 4.3 | clients | created_at | timestamp with time zone, not_nullable, default now() | Client creation time; Can't update |
| 4 | 4.4 | clients | deleted_at | timestamp with time zone | Reserved for company soft deletion; no public company-delete API; System-managed; can't update directly |
| 5 | 5.1 | delivery_attempt_projections | delivery_id | uuid, not_nullable, fk delivery_projections(id) | Delivery whose HTTP attempt is projected from B; Can't update |
| 5 | 5.2 | delivery_attempt_projections | attempt_number | integer, not_nullable | Attempt sequence within this delivery; example: 1; Can't update |
| 5 | 5.3 | delivery_attempt_projections | http_status | integer | This attempt's HTTP code; example: 503; Can't update |
| 5 | 5.4 | delivery_attempt_projections | error | character varying(1000) | This attempt's failure detail; example: Response read timed out; Can't update |
| 5 | 5.5 | delivery_attempt_projections | started_at | timestamp with time zone, not_nullable | HTTP attempt start time in B; Can't update |
| 5 | 5.6 | delivery_attempt_projections | completed_at | timestamp with time zone, not_nullable | HTTP attempt completion time in B; Can't update |
| 5 | 5.7 | delivery_attempt_projections | response_body | text | Response preview from B; capped at 4096 bytes by application code; Can't update |
| 5 | 5.8 | delivery_attempt_projections | content_type | character varying(256) | Response content type; example: application/json; Can't update |
| 5 | 5.9 | delivery_attempt_projections | response_truncated | boolean, not_nullable, default false | Whether the preview was cut off; example: true; Can't update |
| 6 | 6.1 | delivery_projections | id | uuid, not_nullable, pk | Delivery ID; example: 77777777-7777-7777-7777-777777777777; Can't update |
| 6 | 6.2 | delivery_projections | event_id | uuid, not_nullable, fk received_events(id) | Original accepted event; unchanged for a replay; Can't update |
| 6 | 6.3 | delivery_projections | application_id | uuid, not_nullable, fk applications(id) | Owning application; Can't update |
| 6 | 6.4 | delivery_projections | endpoint_id | uuid, not_nullable, fk endpoints(id) | Target endpoint; original event/endpoint pair is unique only for non-replays; Can't update |
| 6 | 6.5 | delivery_projections | status | character varying(32), not_nullable, default 'PENDING'::character varying | PENDING until final SUCCEEDED/DEAD snapshot from B; terminal outcomes cannot change; System-managed; can't update directly |
| 6 | 6.6 | delivery_projections | attempt_count | integer, not_nullable, default 0 | Final HTTP attempt count from B; remains 0 while pending; example: 3; System-managed; can't update directly |
| 6 | 6.7 | delivery_projections | last_sequence | bigint, not_nullable, default 0 | Last applied B status sequence; older updates cannot overwrite current state; System-managed; can't update directly |
| 6 | 6.8 | delivery_projections | http_status | integer | Latest HTTP response code; example: 200; NULL when no HTTP response exists; System-managed; can't update directly |
| 6 | 6.9 | delivery_projections | last_error | character varying(1000) | Latest delivery failure detail; System-managed; can't update directly |
| 6 | 6.10 | delivery_projections | created_at | timestamp with time zone, not_nullable, default now() | Initial delivery/replay creation time; Can't update |
| 6 | 6.11 | delivery_projections | updated_at | timestamp with time zone, not_nullable, default now() | Final B feed timestamp, or initial creation time while pending; System-managed; can't update directly |
| 6 | 6.12 | delivery_projections | endpoint_url | character varying(2048) | Destination URL snapshot for historical display; Can't update |
| 6 | 6.13 | delivery_projections | replay_of_id | uuid, fk delivery_projections(id) | Old failed delivery for this replay; NULL for an initial delivery; Can't update |
| 7 | 7.1 | endpoint_event_types | endpoint_id | uuid, not_nullable, fk endpoints(id) | Current subscriptions; old/new mappings recorded in audit; In place; ADMIN; audit |
| 7 | 7.2 | endpoint_event_types | event_type_id | uuid, not_nullable, fk event_types(id) | Same-application event-type subscription; changes are audited in place; In place; ADMIN; audit |
| 8 | 8.1 | endpoints | id | uuid, not_nullable, pk | Endpoint ID; example: 44444444-4444-4444-4444-444444444444; Can't update |
| 8 | 8.2 | endpoints | application_id | uuid, not_nullable, fk applications(id) | Owning application; Can't update |
| 8 | 8.3 | endpoints | url | character varying(2048), not_nullable | Current destination; old/new URL recorded in audit; example: https://example.com/webhooks/orders; In place; ADMIN; audit |
| 8 | 8.4 | endpoints | description | character varying(500) | Editable description; example: Order updates; In place; any app member; no audit |
| 8 | 8.5 | endpoints | created_at | timestamp with time zone, not_nullable, default now() | Endpoint creation time; Can't update |
| 8 | 8.6 | endpoints | deleted_at | timestamp with time zone | Soft-delete time; subscriptions and delivery history retained; System-managed; can't update directly |
| 8 | 8.7 | endpoints | is_active | boolean, not_nullable, default true | Temporary pause; false excludes new deliveries and blocks replay; configuration.enabled aliases this flag; In place; ADMIN; audit |
| 8 | 8.8 | endpoints | rate_limit_per_second | integer, not_nullable, default 10 | Delivery rate setting copied into new commands; example: 10; In place; any app member; audit |
| 8 | 8.9 | endpoints | concurrency_limit | integer, not_nullable, default 4 | Delivery concurrency setting copied into new commands; example: 4; In place; any app member; audit |
| 8 | 8.10 | endpoints | timeout_ms | integer, not_nullable, default 10000 | Webhook timeout in milliseconds; example: 10000; In place; any app member; audit |
| 8 | 8.11 | endpoints | max_attempts | integer, not_nullable, default 5 | Maximum HTTP attempts including the first; example: 5; In place; any app member; audit |
| 8 | 8.12 | endpoints | signing_secret_encrypted | text | Encrypted current signing credential; accepted commands retain encrypted snapshots; never exposed in audit; System-managed; can't update directly |
| 8 | 8.13 | endpoints | signing_secret_version | integer, not_nullable, default 0 | Secret rotation counter; API-created endpoints start at 1; System-managed; can't update directly |
| 8 | 8.14 | endpoints | signing_secret_rotated_at | timestamp with time zone | Last signing-secret rotation time; System-managed; can't update directly |
| 8 | 8.15 | endpoints | name | character varying(120), not_nullable, default 'Endpoint'::character varying | Editable display name; example: Order webhook; API creation without name uses destination host; In place; any app member; audit |
| 9 | 9.1 | event_types | id | uuid, not_nullable, pk | Event-type ID; example: 33333333-3333-3333-3333-333333333333; Can't update |
| 9 | 9.2 | event_types | application_id | uuid, not_nullable, fk applications(id) | Owning application; Can't update |
| 9 | 9.3 | event_types | name | character varying(120), not_nullable | Immutable routing name; example: order.created; reserved after deletion; Can't update; routing contract |
| 9 | 9.4 | event_types | description | character varying(500) | Editable description; example: A new order was created; In place; any app member; no audit |
| 9 | 9.5 | event_types | created_at | timestamp with time zone, not_nullable, default now() | Event-type creation time; Can't update |
| 9 | 9.6 | event_types | deleted_at | timestamp with time zone | Soft-delete time; existing events/deliveries remain visible; System-managed; can't update directly |
| 9 | 9.7 | event_types | is_active | boolean, not_nullable, default true | Temporary pause; false blocks new events of this type and related replays; In place; ADMIN; audit |
| 10 | 10.1 | incoming_requests | id | uuid, not_nullable, pk | Request ID saved before validation/ingestion; accepted events reference it; Can't update |
| 10 | 10.2 | incoming_requests | application_id | uuid, fk applications(id) | Existing target app; NULL for unknown or invalid application IDs; Can't update |
| 10 | 10.3 | incoming_requests | requested_application_id | character varying(100), not_nullable | Requested UUID or INVALID_APPLICATION_ID; unassignable requests appear in admin-wide history; Can't update |
| 10 | 10.4 | incoming_requests | received_at | timestamp with time zone, not_nullable | Time the HTTP event submission reached Service A; Can't update |
| 10 | 10.5 | incoming_requests | duration_ms | bigint, not_nullable | Request processing duration in milliseconds, excluding audit persistence; System-managed; can't update directly |
| 10 | 10.6 | incoming_requests | http_status | integer, not_nullable | Actual response status; example: 400 for missing idempotency key; System-managed; can't update directly |
| 10 | 10.7 | incoming_requests | outcome | character varying(16), not_nullable | Request lifecycle; rejections remain audit-only, never deliveries/DLQ/replays; System-managed; can't update directly |
| 10 | 10.8 | incoming_requests | reason_code | character varying(64), not_nullable | Stable classification; examples: IDEMPOTENCY_KEY_MISSING, MALFORMED_REQUEST, APPLICATION_INACTIVE, EVENT_TYPE_INACTIVE; System-managed; can't update directly |
| 10 | 10.9 | incoming_requests | reason | character varying(300), not_nullable | Sanitized reason; never an exception dump or raw request body; System-managed; can't update directly |
| 10 | 10.10 | incoming_requests | event_type | character varying(120) | Validated event-type name; unavailable when rejected before validated ingestion; System-managed; can't update directly |
| 10 | 10.11 | incoming_requests | auth_method | character varying(16), not_nullable | Credential type supplied; no credential values are stored; Can't update |
| 10 | 10.12 | incoming_requests | idempotency_key_present | boolean, not_nullable | Whether a nonblank idempotency key was supplied; header value is not stored; Can't update |
| 10 | 10.13 | incoming_requests | client_ip | character varying(64), not_nullable | Direct client IP; example: 127.0.0.1; Can't update |
| 10 | 10.14 | incoming_requests | skipped_endpoints | jsonb, not_nullable, default '[]'::jsonb | Inactive endpoint UUID snapshots; no delivery is created for these targets; example: ["44444444-4444-4444-4444-444444444444"]; System-managed; can't update directly |
| 10 | 10.15 | incoming_requests | api_key_id | uuid, fk api_keys(id) | Matched key for this target app, including rejected revoked/deleted keys; never identifies another app's key; Can't update |
| 10 | 10.16 | incoming_requests | api_key_name | character varying(120) | Key-name snapshot at submission time; searchable even after later rename/revocation; Can't update |
| 10 | 10.17 | incoming_requests | duplicate_of_id | uuid, fk incoming_requests(id) | Original incoming request when returning an existing idempotent event; API eventId is derived through received_events; System-managed; can't update directly |
| 11 | 11.1 | login_attempts | id | uuid, not_nullable, pk | Login audit entry ID; Can't update |
| 11 | 11.2 | login_attempts | user_id | uuid, fk users(id) | Attempted user; NULL for an unknown account; Can't update |
| 11 | 11.3 | login_attempts | email_hash | character varying(64), not_nullable | SHA-256 of normalized attempted email; Can't update |
| 11 | 11.4 | login_attempts | attempted_at | timestamp with time zone, not_nullable, default now() | Login attempt time; Can't update |
| 11 | 11.5 | login_attempts | reason | character varying(32), not_nullable | Outcome: SUCCESS, BAD_CREDENTIALS, LOCKED or ROLE_DENIED; enforced by code; Can't update |
| 11 | 11.6 | login_attempts | ip_address | character varying(64), not_nullable | Client address; example: 127.0.0.1; Can't update |
| 12 | 12.1 | login_guards | email_hash | character varying(64), not_nullable, pk | SHA-256 of normalized email; also tracks unknown accounts; Can't update |
| 12 | 12.2 | login_guards | failures | integer, not_nullable, default 0 | Failed-password counter; example: 5 triggers lockout; System-managed; can't update directly |
| 12 | 12.3 | login_guards | window_started_at | timestamp with time zone, not_nullable, default now() | Start of the 15-minute failure window; System-managed; can't update directly |
| 12 | 12.4 | login_guards | locked_until | timestamp with time zone | Login blocked until this time; example: now + 15 minutes; System-managed; can't update directly |
| 13 | 13.1 | outbox_messages | id | uuid, not_nullable, pk | Outgoing RabbitMQ message ID; stable across publish retries; Can't update |
| 13 | 13.2 | outbox_messages | aggregate_id | uuid, not_nullable | Delivery ID for this outgoing command; logical reference, no DB FK; Can't update |
| 13 | 13.3 | outbox_messages | exchange_name | character varying(120), not_nullable | Destination exchange; example: event-relay.delivery; Can't update |
| 13 | 13.4 | outbox_messages | routing_key | character varying(120), not_nullable | Message route; example: delivery.requested; Can't update |
| 13 | 13.5 | outbox_messages | payload | jsonb, not_nullable | JSON command containing body, endpoint configuration and encrypted signing-secret snapshots; Can't update |
| 13 | 13.6 | outbox_messages | attempt_count | integer, not_nullable, default 0 | Broker publish claims/attempts; not webhook HTTP attempts; System-managed; can't update directly |
| 13 | 13.7 | outbox_messages | available_at | timestamp with time zone, not_nullable, default now() | Earliest time the publisher may claim this message; System-managed; can't update directly |
| 13 | 13.8 | outbox_messages | locked_until | timestamp with time zone | Publisher claim lease expiry; another worker can reclaim afterwards; System-managed; can't update directly |
| 13 | 13.9 | outbox_messages | claim_token | uuid | Publisher ownership token; prevents an expired worker from changing another claim; System-managed; can't update directly |
| 13 | 13.10 | outbox_messages | published_at | timestamp with time zone | Broker-confirmed publication time; NULL means still pending; System-managed; can't update directly |
| 13 | 13.11 | outbox_messages | last_error | character varying(200) | Last publish exception class; example: TimeoutException; System-managed; can't update directly |
| 13 | 13.12 | outbox_messages | created_at | timestamp with time zone, not_nullable, default now() | Outgoing message creation time; commits with event/replay changes; Can't update |
| 14 | 14.1 | received_events | id | uuid, not_nullable, pk | Accepted event ID; example: 66666666-6666-6666-6666-666666666666; Can't update |
| 14 | 14.2 | received_events | application_id | uuid, not_nullable, fk applications(id) | Application receiving the event; Can't update |
| 14 | 14.3 | received_events | event_type_id | uuid, not_nullable, fk event_types(id) | Original event-type reference; Can't update |
| 14 | 14.4 | received_events | event_type | character varying(120), not_nullable | Routing-name snapshot; example: order.created; Can't update |
| 14 | 14.5 | received_events | idempotency_key | character varying(200), not_nullable | Required event request key; example: order-created-123; same request returns its original receipt; Can't update |
| 14 | 14.6 | received_events | request_hash | character varying(64), not_nullable | SHA-256 of event type + canonical payload; detects conflicting key reuse; Can't update |
| 14 | 14.7 | received_events | payload | jsonb, not_nullable | Original JSON payload; example: {"orderId":"123","amount":500}; Can't update |
| 14 | 14.8 | received_events | delivery_count | integer, not_nullable | Initial active destination count; excludes later replays; Can't update |
| 14 | 14.9 | received_events | created_at | timestamp with time zone, not_nullable | Event acceptance time; Can't update |
| 14 | 14.10 | received_events | incoming_request_id | uuid, not_nullable, unique, fk incoming_requests(id) | Originating HTTP request; committed before event acceptance; duplicate submissions reuse this event; Can't update |
| 15 | 15.1 | reconciliation_cursors | name | varchar(80), pk, not_nullable | Worker name; example: service-b-completions; Can't update |
| 15 | 15.2 | reconciliation_cursors | finalized_at | timestamptz, not_nullable, default epoch | Last imported B completion timestamp; example: 2026-11-09T12:00:00Z; System-managed; advance only after successful page |
| 15 | 15.3 | reconciliation_cursors | delivery_id | uuid, not_nullable, default zero UUID | Tie-breaker for equal timestamps; no cross-service FK; System-managed; advance with timestamp |
| 15 | 15.4 | reconciliation_cursors | updated_at | timestamptz, not_nullable, default now() | Checkpoint commit time; example: 2026-11-09T12:01:00Z; System-managed; in place with checkpoint |
| 16 | 16.1 | refresh_token_families | id | uuid, not_nullable, pk | One browser login family; example: 99999999-9999-4999-8999-999999999999; Can't update |
| 16 | 16.2 | refresh_token_families | user_id | uuid, not_nullable, fk users(id) | Account whose access may be renewed; example: ffb80553-fc3a-4573-94ab-53d9e0270b53; Can't update |
| 16 | 16.3 | refresh_token_families | created_at | timestamp with time zone, not_nullable, default now() | Login time; example: 2026-11-09T10:00:00Z; Can't update |
| 16 | 16.4 | refresh_token_families | expires_at | timestamp with time zone, not_nullable | Absolute expiry; never extended; caps renewed JWT expiry; example: 2026-12-09T10:00:00Z; Can't update |
| 16 | 16.5 | refresh_token_families | revoked_at | timestamp with time zone | Family invalidation; example: NULL; System-managed; can't update directly |
| 16 | 16.6 | refresh_token_families | revocation_reason | character varying(32) | REUSE_DETECTED, LOGOUT, PASSWORD_CHANGED, ACCOUNT_DELETED, ACCOUNT_UNAVAILABLE or EXPIRED; example: NULL; System-managed; can't update directly |
| 17 | 17.1 | refresh_tokens | id | uuid, not_nullable, pk | Credential record; example: 88888888-8888-4888-8888-888888888888; Can't update |
| 17 | 17.2 | refresh_tokens | family_id | uuid, not_nullable, fk refresh_token_families(id) | Shared fixed expiry and revocation; example: 99999999-9999-4999-8999-999999999999; Can't update |
| 17 | 17.3 | refresh_tokens | token_hash | character varying(64), not_nullable, unique | SHA-256 only; random 256-bit opaque value stays in HttpOnly cookie; example: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa; Can't update |
| 17 | 17.4 | refresh_tokens | created_at | timestamp with time zone, not_nullable, default now() | Issued at login/rotation; example: 2026-11-09T10:15:00Z; Can't update |
| 17 | 17.5 | refresh_tokens | used_at | timestamp with time zone | Set on rotation; retain hash so later reuse revokes entire family; example: NULL; System-managed; can't update directly |
| 18 | 18.1 | replay_requests | application_id | uuid, not_nullable, fk applications(id) | Application requesting replay; scopes the idempotency key; Can't update |
| 18 | 18.2 | replay_requests | idempotency_key | character varying(200), not_nullable | Replay request key; separate from event key; example: replay-delivery-123; Can't update |
| 18 | 18.3 | replay_requests | delivery_id | uuid, not_nullable, unique, fk delivery_projections(id) | New replay delivery; retries of this request return this existing receipt; Can't update; original delivery derived from delivery_projections.replay_of_id |
| 18 | 18.4 | replay_requests | created_at | timestamp with time zone, not_nullable | Replay request acceptance time; Can't update |
| 19 | 19.1 | status_inbox | message_id | uuid, not_nullable, pk | Incoming B status-message ID; duplicate messages are ignored; Can't update |
| 19 | 19.2 | status_inbox | delivery_id | uuid, not_nullable, fk delivery_projections(id) | Projection updated by this B status message; Can't update |
| 19 | 19.3 | status_inbox | processed_at | timestamp with time zone, not_nullable, default now() | Time Service A processed the status message; Can't update |
| 20 | 20.1 | users | id | uuid, not_nullable, pk | Account ID; example: 11111111-1111-1111-1111-111111111111; Can't update |
| 20 | 20.2 | users | client_id | uuid, not_nullable, fk clients(id) | Single company boundary; example: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa; Can't update |
| 20 | 20.3 | users | name | character varying(120), not_nullable | Display name; example: Alice; In place; self or company ADMIN; no audit |
| 20 | 20.4 | users | email | character varying(320), not_nullable, unique | Global login email; normalized; unique and reserved after deletion; example: alice@example.com; In place; ADMIN in own company; audit |
| 20 | 20.5 | users | created_at | timestamp with time zone, not_nullable, default now() | Registration time; Can't update |
| 20 | 20.6 | users | password_hash | character varying(255), not_nullable | PBKDF2 hash; self change requires current password; company ADMIN can reset; revokes all refresh families; audit records PASSWORD_CHANGED only; no password/hash values; In place; self or company ADMIN; audit without values |
| 20 | 20.7 | users | deleted_at | timestamp with time zone | Soft deletion; mappings/refresh revoked; company apps remain; last company ADMIN protected; System-managed; can't update directly |
| 20 | 20.8 | users | role | character varying(16), not_nullable, default 'USER'::character varying NOT NULL, check USER/ADMIN | Only USER or ADMIN; same role across company apps; example: ADMIN; In place; ADMIN in own company; audit |
| 21 | 21.1 | workspace_members | id | uuid, not_nullable, pk | App mapping ID; Can't update |
| 21 | 21.2 | workspace_members | client_id | uuid, not_nullable, fk users(id,client_id), fk applications(id,client_id) | Composite FKs prevent cross-company app mappings; Can't update |
| 21 | 21.3 | workspace_members | application_id | uuid, not_nullable, fk applications(id,client_id) | Mapped application; example: 22222222-2222-2222-2222-222222222222; Can't update |
| 21 | 21.4 | workspace_members | user_id | uuid, not_nullable, fk users(id,client_id) | Explicit USER app access; ADMIN needs no mappings; Can't update |
| 21 | 21.5 | workspace_members | created_at | timestamp with time zone, not_nullable, default now() | First mapping creation time; Can't update |
| 21 | 21.6 | workspace_members | deleted_at | timestamp with time zone | Mapping removed softly; permission revoked immediately; System-managed; can't update directly |

Endpoint edits update in place. Audit actions distinguish ENDPOINT_UPDATED, CONFIG_UPDATED and EVENT_TYPE_MAPPINGS_UPDATED. Accepted commands retain their destination, credential and delivery settings; retries use the same snapshot.
