CREATE SCHEMA IF NOT EXISTS service_a;

CREATE TABLE IF NOT EXISTS service_a.users (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL,
    email varchar(320) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (email = lower(email))
);

-- Upgrade the existing registration-only schema (users table verified empty).
ALTER TABLE service_a.users ADD COLUMN IF NOT EXISTS password_hash varchar(255) NOT NULL;

CREATE TABLE IF NOT EXISTS service_a.applications (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES service_a.users(id),
    name varchar(120) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS service_a.event_types (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES service_a.applications(id),
    name varchar(120) NOT NULL,
    description varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (application_id, name)
);

CREATE TABLE IF NOT EXISTS service_a.endpoints (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES service_a.applications(id),
    url varchar(2048) NOT NULL,
    description varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (application_id, url)
);

CREATE TABLE IF NOT EXISTS service_a.endpoint_event_types (
    endpoint_id uuid NOT NULL REFERENCES service_a.endpoints(id),
    event_type_id uuid NOT NULL REFERENCES service_a.event_types(id),
    PRIMARY KEY (endpoint_id, event_type_id)
);

CREATE INDEX IF NOT EXISTS idx_endpoint_event_types_event ON service_a.endpoint_event_types(event_type_id);

CREATE TABLE IF NOT EXISTS service_a.api_keys (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES service_a.applications(id),
    name varchar(120) NOT NULL,
    key_hash varchar(64) NOT NULL UNIQUE,
    key_prefix varchar(16) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    replaced_by_id uuid REFERENCES service_a.api_keys(id),
    CHECK (replaced_by_id IS NULL OR (revoked_at IS NOT NULL AND replaced_by_id <> id))
);

CREATE INDEX IF NOT EXISTS idx_api_keys_application ON service_a.api_keys(application_id);

-- Additive upgrades; existing rows and subscriptions are retained.
ALTER TABLE service_a.users ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE service_a.applications ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE service_a.event_types ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE service_a.api_keys ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS rate_limit_per_second integer NOT NULL DEFAULT 10 CHECK (rate_limit_per_second BETWEEN 1 AND 100000);
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS concurrency_limit integer NOT NULL DEFAULT 4 CHECK (concurrency_limit BETWEEN 1 AND 1000);
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS timeout_ms integer NOT NULL DEFAULT 10000 CHECK (timeout_ms BETWEEN 100 AND 60000);
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS max_attempts integer NOT NULL DEFAULT 5 CHECK (max_attempts BETWEEN 1 AND 20);
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS signing_secret_encrypted text;
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS signing_secret_version integer NOT NULL DEFAULT 0;
ALTER TABLE service_a.endpoints ADD COLUMN IF NOT EXISTS signing_secret_rotated_at timestamptz;


CREATE TABLE IF NOT EXISTS service_a.received_events (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES service_a.applications(id),
    event_type_id uuid NOT NULL REFERENCES service_a.event_types(id),
    event_type varchar(120) NOT NULL,
    idempotency_key varchar(200) NOT NULL,
    request_hash varchar(64) NOT NULL,
    payload jsonb NOT NULL,
    delivery_count integer NOT NULL,
    created_at timestamptz NOT NULL,
    deleted_at timestamptz,
    UNIQUE (application_id, idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_received_events_app ON service_a.received_events(application_id, created_at);

CREATE TABLE IF NOT EXISTS service_a.delivery_projections (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES service_a.received_events(id),
    application_id uuid NOT NULL REFERENCES service_a.applications(id),
    endpoint_id uuid NOT NULL REFERENCES service_a.endpoints(id),
    status varchar(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCEEDED','RETRY_SCHEDULED','DEAD')),
    attempt_count integer NOT NULL DEFAULT 0,
    last_sequence bigint NOT NULL DEFAULT 0,
    http_status integer,
    last_error varchar(1000),
    next_attempt_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    UNIQUE (event_id, endpoint_id)
);
CREATE INDEX IF NOT EXISTS idx_projection_event ON service_a.delivery_projections(event_id);
CREATE TABLE IF NOT EXISTS service_a.status_inbox (
    message_id uuid PRIMARY KEY,
    delivery_id uuid NOT NULL REFERENCES service_a.delivery_projections(id),
    processed_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS service_a.outbox_messages (
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
CREATE INDEX IF NOT EXISTS idx_outbox_pending ON service_a.outbox_messages (available_at, created_at)
    WHERE published_at IS NULL AND deleted_at IS NULL;

-- Manual replay creates a linked delivery; original history remains immutable.
ALTER TABLE service_a.delivery_projections ADD COLUMN IF NOT EXISTS endpoint_url varchar(2048);
ALTER TABLE service_a.delivery_projections ADD COLUMN IF NOT EXISTS replay_of_id uuid REFERENCES service_a.delivery_projections(id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_initial_delivery_target ON service_a.delivery_projections(event_id, endpoint_id) WHERE replay_of_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_delivery_replay_source ON service_a.delivery_projections(replay_of_id) WHERE replay_of_id IS NOT NULL;
ALTER TABLE service_a.delivery_projections DROP CONSTRAINT IF EXISTS delivery_projections_event_id_endpoint_id_key;
CREATE INDEX IF NOT EXISTS idx_delivery_activity ON service_a.delivery_projections(application_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_delivery_activity_status ON service_a.delivery_projections(application_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS service_a.delivery_attempt_projections (
    delivery_id uuid NOT NULL REFERENCES service_a.delivery_projections(id),
    attempt_number integer NOT NULL,
    http_status integer,
    error varchar(1000),
    started_at timestamptz NOT NULL,
    completed_at timestamptz NOT NULL,
    response_body text,
    content_type varchar(256),
    response_truncated boolean NOT NULL DEFAULT false,
    PRIMARY KEY (delivery_id, attempt_number)
);
CREATE TABLE IF NOT EXISTS service_a.replay_requests (
    application_id uuid NOT NULL REFERENCES service_a.applications(id),
    idempotency_key varchar(200) NOT NULL,
    source_delivery_id uuid NOT NULL UNIQUE REFERENCES service_a.delivery_projections(id),
    delivery_id uuid NOT NULL UNIQUE REFERENCES service_a.delivery_projections(id),
    created_at timestamptz NOT NULL,
    PRIMARY KEY (application_id, idempotency_key)
);

-- Dashboard authentication. Existing users remain USER; no automatic administrator.
ALTER TABLE service_a.users ADD COLUMN IF NOT EXISTS role varchar(16) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN'));
CREATE TABLE IF NOT EXISTS service_a.auth_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES service_a.users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    ip_address varchar(64) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_user ON service_a.auth_sessions(user_id);
CREATE TABLE IF NOT EXISTS service_a.login_guards (
    email_hash varchar(64) PRIMARY KEY,
    failures integer NOT NULL DEFAULT 0,
    window_started_at timestamptz NOT NULL DEFAULT now(),
    locked_until timestamptz
);
CREATE TABLE IF NOT EXISTS service_a.login_attempts (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES service_a.users(id),
    email_hash varchar(64) NOT NULL,
    attempted_at timestamptz NOT NULL DEFAULT now(),
    success boolean NOT NULL,
    reason varchar(32) NOT NULL,
    ip_address varchar(64) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_login_attempts_user_time ON service_a.login_attempts(user_id,attempted_at DESC);
