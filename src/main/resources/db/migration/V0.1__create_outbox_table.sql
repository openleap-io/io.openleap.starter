CREATE SEQUENCE outbox_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE outbox (
    id BIGINT PRIMARY KEY,

    business_id UUID NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,

    exchange_key VARCHAR(256) NOT NULL,
    routing_key VARCHAR(256) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_error VARCHAR(4000),
    payload_json jsonb NOT NULL,
    headers_json jsonb,

    CONSTRAINT uk_outbox_business_id UNIQUE (business_id)
);

-- Additional indexes for common query patterns
-- TODO (itaseski): Add indexes based on usage patterns