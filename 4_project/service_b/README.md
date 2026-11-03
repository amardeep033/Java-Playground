# Service B · webhook delivery

Java 21 + Spring Boot. PostgreSQL: `localhost:5432/postgres`, `postgres`/`postgres`; schema `service_b` is created on startup.

From the project root, in separate terminals:

```bash
./run.sh
./run1.sh
./run2.sh
```

The broker launcher uses Docker directly and keeps a persistent named volume. Compose is optional: `docker compose -f rabbitmq.compose.yml up -d`. Use one launcher, since both bind the same ports.

Start A before B: A initializes `service-a/.local/signing.key`; B reads the same file. Override both with the same `SIGNING_SECRET_KEY_FILE` (absolute path) or `SIGNING_SECRET_ENCRYPTION_KEY` (Base64, 32 bytes). Preserve the key across restarts.

- Receive delivery jobs from RabbitMQ; POST JSON to the endpoint; write attempts and status to this service’s own schema.
- Manual ACK after DB commit. Inbox + delivery row lock deduplicate broker redelivery. Remote HTTP is at-least-once; receivers should honor `Idempotency-Key`.
- Endpoint URL/settings/encrypted secret are snapshotted at ingestion. Later endpoint edits/deletion apply only to future events.
- Timeout, maximum attempts, rate and concurrency settings are used. Rate/concurrency limits are **per B instance**; a shared Redis limiter is a later phase.
- Retry: I/O/timeout, 408/425/429, 5xx. Fixed 30s delay; stop at the endpoint’s maximum attempts. Other HTTP failures and redirects go to DLQ.
- Status updates return to A through B’s outbox. Query A’s event API to see results.
- Secrets/ciphertext are never included in status APIs or logs. Bounded text response previews (4 KB) are projected to A with attempt metadata.

RabbitMQ defaults: `localhost:5672`, `guest`/`guest`; management UI: http://localhost:15672. Environment: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`. Set `RABBITMQ_RETRY_DELAY_MS` identically in both services when changing the retry queue TTL.

Tests: `mvn test`. Service B internal HTTP: `127.0.0.1:8082`. gRPC and distributed limiting remain future steps. Manual replay is exposed through Service A and creates a new linked delivery.

Each run script contains its own stop/restart handling. Add `--stop` to stop only that component: `./run.sh --stop`, `./run0.sh --stop`, `./run1.sh --stop`, `./run2.sh --stop`. The infrastructure script restarts RabbitMQ and checks the existing PostgreSQL server; database data is retained.

Older attempt metadata is synchronized to A by `AttemptHistoryPublisher`; this never resends a webhook. Both services must be restarted after this monitoring upgrade.
