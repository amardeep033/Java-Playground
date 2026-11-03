CREATE SCHEMA IF NOT EXISTS service_b;

CREATE TABLE IF NOT EXISTS service_b.deliveries (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL,
    application_id uuid NOT NULL,
    endpoint_id uuid NOT NULL,
    command jsonb NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCEEDED','RETRY_SCHEDULED','DEAD')),
    attempt_count integer NOT NULL DEFAULT 0,
    status_sequence bigint NOT NULL DEFAULT 0,
    http_status integer,
    last_error varchar(1000),
    next_attempt_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    UNIQUE (event_id, endpoint_id)
);
CREATE INDEX IF NOT EXISTS idx_deliveries_event ON service_b.deliveries(event_id);
CREATE TABLE IF NOT EXISTS service_b.delivery_attempts (
    id uuid PRIMARY KEY,
    delivery_id uuid NOT NULL REFERENCES service_b.deliveries(id),
    attempt_number integer NOT NULL,
    http_status integer,
    error varchar(1000),
    started_at timestamptz NOT NULL,
    completed_at timestamptz NOT NULL,
    UNIQUE (delivery_id, attempt_number)
);
CREATE TABLE IF NOT EXISTS service_b.delivery_inbox (
    message_id uuid PRIMARY KEY,
    delivery_id uuid NOT NULL REFERENCES service_b.deliveries(id),
    processed_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS service_b.outbox_messages (
    id uuid PRIMARY KEY,
    aggregate_id uuid NOT NULL,
    exchange_name varchar(120) NOT NULL,
    routing_key varchar(120) NOT NULL,
    payload jsonb NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    available_at timestamptz NOT NULL DEFAULT now(),
    locked_until timestamptz,
    claim_token uuid,
    published_at timestamptz,
    last_error varchar(200),
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_outbox_pending ON service_b.outbox_messages (available_at, created_at)
    WHERE published_at IS NULL AND deleted_at IS NULL;

ALTER TABLE service_b.deliveries ADD COLUMN IF NOT EXISTS replay_of_id uuid;
CREATE UNIQUE INDEX IF NOT EXISTS idx_initial_delivery_target ON service_b.deliveries(event_id, endpoint_id) WHERE replay_of_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_delivery_replay_source ON service_b.deliveries(replay_of_id) WHERE replay_of_id IS NOT NULL;
ALTER TABLE service_b.deliveries DROP CONSTRAINT IF EXISTS deliveries_event_id_endpoint_id_key;
ALTER TABLE service_b.delivery_attempts ADD COLUMN IF NOT EXISTS response_body text;
ALTER TABLE service_b.delivery_attempts ADD COLUMN IF NOT EXISTS content_type varchar(256);
ALTER TABLE service_b.delivery_attempts ADD COLUMN IF NOT EXISTS response_truncated boolean NOT NULL DEFAULT false;

ALTER TABLE service_b.delivery_attempts ADD COLUMN IF NOT EXISTS monitoring_exported_at timestamptz;
