CREATE SCHEMA IF NOT EXISTS service_b;

CREATE TABLE IF NOT EXISTS service_b.deliveries (
    id uuid NOT NULL,
    event_id uuid NOT NULL,
    application_id uuid NOT NULL,
    endpoint_id uuid NOT NULL,
    command jsonb NOT NULL,
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    is_final boolean DEFAULT false NOT NULL,
    finalized_at timestamp with time zone,
    completion_message_id uuid,
    attempt_count integer DEFAULT 0 NOT NULL,
    status_sequence bigint DEFAULT 0 NOT NULL,
    http_status integer,
    last_error character varying(1000),
    next_attempt_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    replay_of_id uuid,
    CONSTRAINT deliveries_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCEEDED'::character varying, 'RETRY_SCHEDULED'::character varying, 'DEAD'::character varying])::text[]))),
    CONSTRAINT deliveries_final_check CHECK (is_final = (status IN ('SUCCEEDED','DEAD')) AND is_final = (finalized_at IS NOT NULL) AND is_final = (completion_message_id IS NOT NULL)),
    CONSTRAINT deliveries_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service_b.delivery_attempts (
    id uuid NOT NULL,
    delivery_id uuid NOT NULL,
    attempt_number integer NOT NULL,
    http_status integer,
    error character varying(1000),
    started_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone NOT NULL,
    response_body text,
    content_type character varying(256),
    response_truncated boolean DEFAULT false NOT NULL,
    CONSTRAINT delivery_attempts_delivery_id_attempt_number_key UNIQUE (delivery_id, attempt_number),
    CONSTRAINT delivery_attempts_pkey PRIMARY KEY (id),
    CONSTRAINT delivery_attempts_delivery_id_fkey FOREIGN KEY (delivery_id) REFERENCES service_b.deliveries(id)
);

CREATE TABLE IF NOT EXISTS service_b.delivery_inbox (
    message_id uuid NOT NULL,
    delivery_id uuid NOT NULL,
    processed_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT delivery_inbox_pkey PRIMARY KEY (message_id),
    CONSTRAINT delivery_inbox_delivery_id_fkey FOREIGN KEY (delivery_id) REFERENCES service_b.deliveries(id)
);

CREATE TABLE IF NOT EXISTS service_b.outbox_messages (
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

CREATE INDEX IF NOT EXISTS idx_deliveries_event ON service_b.deliveries USING btree (event_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_delivery_replay_source ON service_b.deliveries USING btree (replay_of_id) WHERE (replay_of_id IS NOT NULL);

CREATE UNIQUE INDEX IF NOT EXISTS idx_initial_delivery_target ON service_b.deliveries USING btree (event_id, endpoint_id) WHERE (replay_of_id IS NULL);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON service_b.outbox_messages USING btree (available_at, created_at) WHERE (published_at IS NULL);

CREATE UNIQUE INDEX IF NOT EXISTS idx_completion_message ON service_b.deliveries(completion_message_id) WHERE is_final;
CREATE INDEX IF NOT EXISTS idx_final_delivery_cursor ON service_b.deliveries(finalized_at,id) WHERE is_final;
