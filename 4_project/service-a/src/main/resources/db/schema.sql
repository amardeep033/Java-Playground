-- Fresh schema: two client-scoped roles; public signup creates a company and its first ADMIN.

CREATE SCHEMA IF NOT EXISTS service_a;

CREATE TABLE IF NOT EXISTS service_a.clients (
    id uuid NOT NULL,
    name character varying(120) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT clients_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service_a.login_guards (
    email_hash character varying(64) NOT NULL,
    failures integer DEFAULT 0 NOT NULL,
    window_started_at timestamp with time zone DEFAULT now() NOT NULL,
    locked_until timestamp with time zone,
    CONSTRAINT login_guards_pkey PRIMARY KEY (email_hash)
);

CREATE TABLE IF NOT EXISTS service_a.outbox_messages (
    id uuid NOT NULL,
    aggregate_id uuid NOT NULL,
    exchange_name character varying(120) NOT NULL,
    routing_key character varying(120) NOT NULL,
    payload jsonb NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    available_at timestamp with time zone DEFAULT now() NOT NULL,
    locked_until timestamp with time zone,
    claim_token uuid,
    published_at timestamp with time zone,
    last_error character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT outbox_messages_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service_a.users (
    id uuid NOT NULL,
    client_id uuid NOT NULL,
    name character varying(120) NOT NULL,
    email character varying(320) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    password_hash character varying(255) NOT NULL,
    deleted_at timestamp with time zone,
    role character varying(16) DEFAULT 'USER'::character varying NOT NULL,
    CONSTRAINT users_email_check CHECK (((email)::text = lower((email)::text))),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[]))),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_client_fk FOREIGN KEY (client_id) REFERENCES service_a.clients(id),
    CONSTRAINT users_id_client_unique UNIQUE(id,client_id)
);

CREATE TABLE IF NOT EXISTS service_a.applications (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    name character varying(120) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    is_active boolean DEFAULT true NOT NULL,
    description character varying(500),
    client_id uuid NOT NULL,
    CONSTRAINT applications_pkey PRIMARY KEY (id),
    CONSTRAINT applications_client_id_fkey FOREIGN KEY (client_id) REFERENCES service_a.clients(id),
    CONSTRAINT applications_user_id_fkey FOREIGN KEY (user_id) REFERENCES service_a.users(id),
    CONSTRAINT applications_id_client_unique UNIQUE(id,client_id)
);

CREATE TABLE IF NOT EXISTS service_a.audit_logs (
    id uuid NOT NULL,
    application_id uuid,
    actor_id uuid NOT NULL,
    entity_type character varying(40) NOT NULL,
    entity_id uuid NOT NULL,
    action character varying(64) NOT NULL,
    details jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    client_id uuid NOT NULL,
    CONSTRAINT audit_logs_details_check CHECK ((jsonb_typeof(details) = 'object'::text)),
    CONSTRAINT audit_logs_pkey PRIMARY KEY (id),
    CONSTRAINT audit_logs_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES service_a.users(id),
    CONSTRAINT audit_logs_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id),
    CONSTRAINT audit_logs_client_id_fkey FOREIGN KEY (client_id) REFERENCES service_a.clients(id)
);

CREATE TABLE IF NOT EXISTS service_a.endpoints (
    id uuid NOT NULL,
    application_id uuid NOT NULL,
    url character varying(2048) NOT NULL,
    description character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    is_active boolean DEFAULT true NOT NULL,
    rate_limit_per_second integer DEFAULT 10 NOT NULL,
    concurrency_limit integer DEFAULT 4 NOT NULL,
    timeout_ms integer DEFAULT 10000 NOT NULL,
    max_attempts integer DEFAULT 5 NOT NULL,
    signing_secret_encrypted text,
    signing_secret_version integer DEFAULT 0 NOT NULL,
    signing_secret_rotated_at timestamp with time zone,
    name character varying(120) DEFAULT 'Endpoint'::character varying NOT NULL,
    CONSTRAINT endpoints_concurrency_limit_check CHECK (((concurrency_limit >= 1) AND (concurrency_limit <= 1000))),
    CONSTRAINT endpoints_max_attempts_check CHECK (((max_attempts >= 1) AND (max_attempts <= 20))),
    CONSTRAINT endpoints_rate_limit_per_second_check CHECK (((rate_limit_per_second >= 1) AND (rate_limit_per_second <= 100000))),
    CONSTRAINT endpoints_timeout_ms_check CHECK (((timeout_ms >= 100) AND (timeout_ms <= 60000))),
    CONSTRAINT endpoints_application_id_url_key UNIQUE (application_id, url),
    CONSTRAINT endpoints_pkey PRIMARY KEY (id),
    CONSTRAINT endpoints_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id)
);

CREATE TABLE IF NOT EXISTS service_a.event_types (
    id uuid NOT NULL,
    application_id uuid NOT NULL,
    name character varying(120) NOT NULL,
    description character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT event_types_application_id_name_key UNIQUE (application_id, name),
    CONSTRAINT event_types_pkey PRIMARY KEY (id),
    CONSTRAINT event_types_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id)
);

CREATE TABLE IF NOT EXISTS service_a.login_attempts (
    id uuid NOT NULL,
    user_id uuid,
    email_hash character varying(64) NOT NULL,
    attempted_at timestamp with time zone DEFAULT now() NOT NULL,
    reason character varying(32) NOT NULL,
    ip_address character varying(64) NOT NULL,
    CONSTRAINT login_attempts_pkey PRIMARY KEY (id),
    CONSTRAINT login_attempts_user_id_fkey FOREIGN KEY (user_id) REFERENCES service_a.users(id)
);

CREATE TABLE IF NOT EXISTS service_a.refresh_token_families (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    revocation_reason character varying(32),
    CONSTRAINT refresh_token_families_check CHECK (((expires_at > created_at) AND (expires_at <= (created_at + '30 days'::interval)))),
    CONSTRAINT refresh_token_families_check1 CHECK (((revoked_at IS NULL) = (revocation_reason IS NULL))),
    CONSTRAINT refresh_token_families_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_token_families_user_id_fkey FOREIGN KEY (user_id) REFERENCES service_a.users(id)
);

CREATE TABLE IF NOT EXISTS service_a.refresh_tokens (
    id uuid NOT NULL,
    family_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    used_at timestamp with time zone,
    CONSTRAINT refresh_tokens_token_hash_check CHECK (((token_hash)::text ~ '^[0-9a-f]{64}$'::text)),
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash),
    CONSTRAINT refresh_tokens_family_id_fkey FOREIGN KEY (family_id) REFERENCES service_a.refresh_token_families(id)
);

CREATE TABLE IF NOT EXISTS service_a.workspace_members (
    id uuid NOT NULL,
    client_id uuid NOT NULL,
    application_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT workspace_members_application_id_user_id_key UNIQUE (application_id, user_id),
    CONSTRAINT workspace_members_pkey PRIMARY KEY (id),
    CONSTRAINT workspace_user_client_fk FOREIGN KEY(user_id,client_id) REFERENCES service_a.users(id,client_id),
    CONSTRAINT workspace_app_client_fk FOREIGN KEY(application_id,client_id) REFERENCES service_a.applications(id,client_id)
);

CREATE TABLE IF NOT EXISTS service_a.api_keys (
    id uuid NOT NULL,
    application_id uuid NOT NULL,
    name character varying(120) NOT NULL,
    key_hash character varying(64) NOT NULL,
    key_prefix character varying(16) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    revoked_at timestamp with time zone,
    replaced_by_id uuid,
    deleted_at timestamp with time zone,
    CONSTRAINT api_keys_check CHECK (((replaced_by_id IS NULL) OR ((revoked_at IS NOT NULL) AND (replaced_by_id <> id)))),
    CONSTRAINT api_keys_key_hash_key UNIQUE (key_hash),
    CONSTRAINT api_keys_pkey PRIMARY KEY (id),
    CONSTRAINT api_keys_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id),
    CONSTRAINT api_keys_replaced_by_id_fkey FOREIGN KEY (replaced_by_id) REFERENCES service_a.api_keys(id)
);

CREATE TABLE IF NOT EXISTS service_a.endpoint_event_types (
    endpoint_id uuid NOT NULL,
    event_type_id uuid NOT NULL,
    CONSTRAINT endpoint_event_types_pkey PRIMARY KEY (endpoint_id, event_type_id),
    CONSTRAINT endpoint_event_types_endpoint_id_fkey FOREIGN KEY (endpoint_id) REFERENCES service_a.endpoints(id),
    CONSTRAINT endpoint_event_types_event_type_id_fkey FOREIGN KEY (event_type_id) REFERENCES service_a.event_types(id)
);

CREATE TABLE IF NOT EXISTS service_a.incoming_requests (
    id uuid NOT NULL,
    application_id uuid,
    requested_application_id character varying(100) NOT NULL,
    received_at timestamp with time zone NOT NULL,
    duration_ms bigint NOT NULL,
    http_status integer NOT NULL,
    outcome character varying(16) NOT NULL,
    reason_code character varying(64) NOT NULL,
    reason character varying(300) NOT NULL,
    event_type character varying(120),
    auth_method character varying(16) NOT NULL,
    idempotency_key_present boolean NOT NULL,
    client_ip character varying(64) NOT NULL,
    skipped_endpoints jsonb DEFAULT '[]'::jsonb NOT NULL,
    api_key_id uuid,
    api_key_name character varying(120),
    duplicate_of_id uuid,
    CONSTRAINT incoming_requests_auth_method_check CHECK (((auth_method)::text = ANY ((ARRAY['API_KEY'::character varying, 'DASHBOARD'::character varying, 'NONE'::character varying])::text[]))),
    CONSTRAINT incoming_requests_duration_ms_check CHECK ((duration_ms >= 0)),
    CONSTRAINT incoming_requests_http_status_check CHECK (((http_status >= 100) AND (http_status <= 599))),
    CONSTRAINT incoming_requests_outcome_check CHECK (((outcome)::text = ANY ((ARRAY['PROCESSING'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'ERROR'::character varying])::text[]))),
    CONSTRAINT incoming_requests_skipped_endpoints_check CHECK ((jsonb_typeof(skipped_endpoints) = 'array'::text)),
    CONSTRAINT incoming_requests_pkey PRIMARY KEY (id),
    CONSTRAINT incoming_requests_api_key_id_fkey FOREIGN KEY (api_key_id) REFERENCES service_a.api_keys(id),
    CONSTRAINT incoming_requests_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id),
    CONSTRAINT incoming_requests_duplicate_of_id_fkey FOREIGN KEY (duplicate_of_id) REFERENCES service_a.incoming_requests(id)
);

CREATE TABLE IF NOT EXISTS service_a.received_events (
    id uuid NOT NULL,
    application_id uuid NOT NULL,
    event_type_id uuid NOT NULL,
    event_type character varying(120) NOT NULL,
    idempotency_key character varying(200) NOT NULL,
    request_hash character varying(64) NOT NULL,
    payload jsonb NOT NULL,
    delivery_count integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    incoming_request_id uuid NOT NULL,
    CONSTRAINT received_events_application_id_idempotency_key_key UNIQUE (application_id, idempotency_key),
    CONSTRAINT received_events_incoming_request_id_key UNIQUE (incoming_request_id),
    CONSTRAINT received_events_pkey PRIMARY KEY (id),
    CONSTRAINT received_events_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id),
    CONSTRAINT received_events_event_type_id_fkey FOREIGN KEY (event_type_id) REFERENCES service_a.event_types(id),
    CONSTRAINT received_events_incoming_request_id_fkey FOREIGN KEY (incoming_request_id) REFERENCES service_a.incoming_requests(id)
);

CREATE TABLE IF NOT EXISTS service_a.delivery_projections (
    id uuid NOT NULL,
    event_id uuid NOT NULL,
    application_id uuid NOT NULL,
    endpoint_id uuid NOT NULL,
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    last_sequence bigint DEFAULT 0 NOT NULL,
    http_status integer,
    last_error character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    endpoint_url character varying(2048),
    replay_of_id uuid,
    CONSTRAINT delivery_projections_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCEEDED'::character varying, 'DEAD'::character varying])::text[]))),
    CONSTRAINT delivery_projections_pkey PRIMARY KEY (id),
    CONSTRAINT delivery_projections_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id),
    CONSTRAINT delivery_projections_endpoint_id_fkey FOREIGN KEY (endpoint_id) REFERENCES service_a.endpoints(id),
    CONSTRAINT delivery_projections_event_id_fkey FOREIGN KEY (event_id) REFERENCES service_a.received_events(id),
    CONSTRAINT delivery_projections_replay_of_id_fkey FOREIGN KEY (replay_of_id) REFERENCES service_a.delivery_projections(id)
);

CREATE TABLE IF NOT EXISTS service_a.replay_requests (
    application_id uuid NOT NULL,
    idempotency_key character varying(200) NOT NULL,
    delivery_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT replay_requests_delivery_id_key UNIQUE (delivery_id),
    CONSTRAINT replay_requests_pkey PRIMARY KEY (application_id, idempotency_key),
    CONSTRAINT replay_requests_application_id_fkey FOREIGN KEY (application_id) REFERENCES service_a.applications(id),
    CONSTRAINT replay_requests_delivery_id_fkey FOREIGN KEY (delivery_id) REFERENCES service_a.delivery_projections(id)
);

CREATE TABLE IF NOT EXISTS service_a.status_inbox (
    message_id uuid NOT NULL,
    delivery_id uuid NOT NULL,
    processed_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT status_inbox_pkey PRIMARY KEY (message_id),
    CONSTRAINT status_inbox_delivery_id_fkey FOREIGN KEY (delivery_id) REFERENCES service_a.delivery_projections(id)
);

CREATE TABLE IF NOT EXISTS service_a.delivery_attempt_projections (
    delivery_id uuid NOT NULL,
    attempt_number integer NOT NULL,
    http_status integer,
    error character varying(1000),
    started_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone NOT NULL,
    response_body text,
    content_type character varying(256),
    response_truncated boolean DEFAULT false NOT NULL,
    CONSTRAINT delivery_attempt_projections_pkey PRIMARY KEY (delivery_id, attempt_number),
    CONSTRAINT delivery_attempt_projections_delivery_id_fkey FOREIGN KEY (delivery_id) REFERENCES service_a.delivery_projections(id)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_application ON service_a.api_keys USING btree (application_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_application_client_name ON service_a.applications USING btree (client_id, name);

CREATE INDEX IF NOT EXISTS idx_applications_client ON service_a.applications USING btree (client_id);

CREATE INDEX IF NOT EXISTS idx_audit_client_time ON service_a.audit_logs USING btree (client_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_audit_workspace_time ON service_a.audit_logs USING btree (application_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_delivery_activity ON service_a.delivery_projections USING btree (application_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_delivery_activity_status ON service_a.delivery_projections USING btree (application_id, status, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_delivery_replay_source ON service_a.delivery_projections USING btree (replay_of_id) WHERE (replay_of_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_endpoint_event_types_event ON service_a.endpoint_event_types USING btree (event_type_id);


CREATE INDEX IF NOT EXISTS idx_incoming_api_key ON service_a.incoming_requests USING btree (api_key_id, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_incoming_requests_app_time ON service_a.incoming_requests USING btree (application_id, received_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_incoming_requests_outcome_time ON service_a.incoming_requests USING btree (outcome, received_at DESC, id DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_initial_delivery_target ON service_a.delivery_projections USING btree (event_id, endpoint_id) WHERE (replay_of_id IS NULL);

CREATE INDEX IF NOT EXISTS idx_login_attempts_user_time ON service_a.login_attempts USING btree (user_id, attempted_at DESC);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON service_a.outbox_messages USING btree (available_at, created_at) WHERE (published_at IS NULL);

CREATE INDEX IF NOT EXISTS idx_projection_event ON service_a.delivery_projections USING btree (event_id);

CREATE INDEX IF NOT EXISTS idx_received_events_app ON service_a.received_events USING btree (application_id, created_at);

CREATE INDEX IF NOT EXISTS idx_workspace_members_user ON service_a.workspace_members USING btree (user_id, application_id) WHERE (deleted_at IS NULL);

CREATE INDEX IF NOT EXISTS refresh_families_user ON service_a.refresh_token_families USING btree (user_id) WHERE (revoked_at IS NULL);

CREATE INDEX IF NOT EXISTS refresh_tokens_family ON service_a.refresh_tokens USING btree (family_id);

CREATE INDEX IF NOT EXISTS users_client_role ON service_a.users(client_id,role) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS service_a.reconciliation_cursors (
    name varchar(80) PRIMARY KEY,
    finalized_at timestamptz NOT NULL DEFAULT '1970-01-01T00:00:00Z',
    delivery_id uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    updated_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO service_a.reconciliation_cursors(name) VALUES ('service-b-completions') ON CONFLICT DO NOTHING;
