| table_number | column_number | table_name | column_name | constraint | usage |
| --- | --- | --- | --- | --- | --- |
| 1 | 1.1 | deliveries | id | uuid, not_nullable, pk | Delivery ID from A; example: 77777777-7777-7777-7777-777777777777; Can't update |
| 1 | 1.2 | deliveries | event_id | uuid, not_nullable, unique(event_id,endpoint_id) for initial deliveries | Original event ID in A; no cross-service FK; example: 66666666-6666-6666-6666-666666666666; Can't update |
| 1 | 1.3 | deliveries | application_id | uuid, not_nullable | Owning app ID in A; no cross-service FK; example: 22222222-2222-2222-2222-222222222222; Can't update |
| 1 | 1.4 | deliveries | endpoint_id | uuid, not_nullable, unique(event_id,endpoint_id) for initial deliveries | Target endpoint ID in A; no cross-service FK; example: 44444444-4444-4444-4444-444444444444; Can't update |
| 1 | 1.5 | deliveries | command | jsonb, not_nullable | Complete immutable dispatch snapshot from A: body, destination, limits and encrypted signing credential; private; example body: {"orderId":"123"}; Can't update |
| 1 | 1.6 | deliveries | status | character varying(32), not_nullable, default 'PENDING'::character varying, check PENDING/RETRY_SCHEDULED/SUCCEEDED/DEAD | PENDING, RETRY_SCHEDULED, SUCCEEDED or DEAD; final outcomes cannot change; example: DEAD; System-managed; can't update directly |
| 1 | 1.7 | deliveries | is_final | boolean, not_nullable, default false, check final-state consistency | True exactly for SUCCEEDED/DEAD; supports final-only feed index; example: true; System-managed; can't update directly |
| 1 | 1.8 | deliveries | finalized_at | timestamp with time zone, check final-state consistency | Final completion timestamp; required only when final; cursor ordered with id; example: 2026-11-10T10:00:00Z; System-managed; can't update directly |
| 1 | 1.9 | deliveries | completion_message_id | uuid, check final-state consistency, unique when final | Stable final message ID; shared by RabbitMQ and reconciliation; required only when final; example: 99999999-9999-4999-8999-999999999999; System-managed; can't update directly |
| 1 | 1.10 | deliveries | attempt_count | integer, not_nullable, default 0 | Completed HTTP attempts; summary for delivery reads; example: 3; System-managed; can't update directly |
| 1 | 1.11 | deliveries | status_sequence | bigint, not_nullable, default 0 | State sequence incremented per completed HTTP attempt; exported to A; example: 3; System-managed; can't update directly |
| 1 | 1.12 | deliveries | http_status | integer | Latest HTTP response code; NULL without a response; example: 503; System-managed; can't update directly |
| 1 | 1.13 | deliveries | last_error | character varying(1000) | Latest failure summary; example: HTTP 503; System-managed; can't update directly |
| 1 | 1.14 | deliveries | next_attempt_at | timestamp with time zone | Scheduled retry time; NULL after final completion; example: 2026-11-10T10:00:30Z; System-managed; can't update directly |
| 1 | 1.15 | deliveries | created_at | timestamp with time zone, not_nullable, default now() | First command receipt time; example: 2026-11-10T09:59:00Z; Can't update |
| 1 | 1.16 | deliveries | updated_at | timestamp with time zone, not_nullable, default now() | Latest result timestamp; final timestamp when complete; example: 2026-11-10T10:00:00Z; System-managed; can't update directly |
| 1 | 1.17 | deliveries | replay_of_id | uuid, unique when not NULL | Old failed delivery ID; NULL for initial delivery; no DB FK; example: 88888888-8888-4888-8888-888888888888; Can't update |
| 2 | 2.1 | delivery_attempts | id | uuid, not_nullable, pk | HTTP attempt ID; example: aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa; Can't update |
| 2 | 2.2 | delivery_attempts | delivery_id | uuid, not_nullable, fk deliveries(id), unique(delivery_id,attempt_number) | Delivery owning this attempt; example: 77777777-7777-7777-7777-777777777777; Can't update |
| 2 | 2.3 | delivery_attempts | attempt_number | integer, not_nullable, unique(delivery_id,attempt_number) | Attempt order within delivery; example: 1; Can't update |
| 2 | 2.4 | delivery_attempts | http_status | integer | This attempt response code; NULL without a response; example: 503; Can't update |
| 2 | 2.5 | delivery_attempts | error | character varying(1000) | This attempt failure detail; example: Response read timed out; Can't update |
| 2 | 2.6 | delivery_attempts | started_at | timestamp with time zone, not_nullable | HTTP attempt start; example: 2026-11-10T09:59:00Z; Can't update |
| 2 | 2.7 | delivery_attempts | completed_at | timestamp with time zone, not_nullable | HTTP attempt end; example: 2026-11-10T09:59:01Z; Can't update |
| 2 | 2.8 | delivery_attempts | response_body | text | Response preview capped at 4096 characters; example: {"error":"unavailable"}; Can't update |
| 2 | 2.9 | delivery_attempts | content_type | character varying(256) | Response content type; example: application/json; Can't update |
| 2 | 2.10 | delivery_attempts | response_truncated | boolean, not_nullable, default false | Whether response preview was shortened; example: false; Can't update |
| 3 | 3.1 | delivery_inbox | message_id | uuid, not_nullable, pk | RabbitMQ command ID for deduplication; retries have new message IDs; example: bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb; Can't update |
| 3 | 3.2 | delivery_inbox | delivery_id | uuid, not_nullable, fk deliveries(id) | Delivery handled by this command; example: 77777777-7777-7777-7777-777777777777; Can't update |
| 3 | 3.3 | delivery_inbox | processed_at | timestamp with time zone, not_nullable, default now() | Command transaction processing time; example: 2026-11-10T09:59:01Z; Can't update |
| 4 | 4.1 | outbox_messages | id | uuid, not_nullable, pk | Outgoing RabbitMQ message ID; stable across publish retries; example: cccccccc-cccc-4ccc-8ccc-cccccccccccc; Can't update |
| 4 | 4.2 | outbox_messages | aggregate_id | uuid, not_nullable | Delivery ID; logical reference without FK; example: 77777777-7777-7777-7777-777777777777; Can't update |
| 4 | 4.3 | outbox_messages | exchange_name | character varying(120), not_nullable | Destination exchange; example: event-relay.status; Can't update |
| 4 | 4.4 | outbox_messages | routing_key | character varying(120), not_nullable | delivery.retry or delivery.completed; example: delivery.completed; Can't update |
| 4 | 4.5 | outbox_messages | payload | jsonb, not_nullable | Complete outgoing retry command or final snapshot with attempt history; example status: DEAD; command credentials stay encrypted; Can't update |
| 4 | 4.6 | outbox_messages | attempt_count | integer, not_nullable, default 0 | Broker publish claims; separate from webhook attempts; example: 2; System-managed; can't update directly |
| 4 | 4.7 | outbox_messages | available_at | timestamp with time zone, not_nullable, default now() | Earliest publish claim time; example: 2026-11-10T10:00:00Z; System-managed; can't update directly |
| 4 | 4.8 | outbox_messages | locked_until | timestamp with time zone | Publisher lease expiry; allows reclaim after a crash; example: 2026-11-10T10:00:30Z; System-managed; can't update directly |
| 4 | 4.9 | outbox_messages | claim_token | uuid | Current publisher ownership token; example: dddddddd-dddd-4ddd-8ddd-dddddddddddd; System-managed; can't update directly |
| 4 | 4.10 | outbox_messages | published_at | timestamp with time zone | Broker-confirmed publication time; NULL while pending; example: 2026-11-10T10:00:01Z; System-managed; can't update directly |
| 4 | 4.11 | outbox_messages | last_error | character varying(200) | Latest publisher exception class; example: TimeoutException; System-managed; can't update directly |
| 4 | 4.12 | outbox_messages | created_at | timestamp with time zone, not_nullable, default now() | Message creation time; commits with delivery state; example: 2026-11-10T10:00:00Z; Can't update |
