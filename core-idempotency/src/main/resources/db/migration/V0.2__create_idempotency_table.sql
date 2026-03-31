CREATE SEQUENCE idempotency_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE idempotency (
    id BIGINT PRIMARY KEY,

    idem_key VARCHAR(128) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    entry_uuid UUID NOT NULL,

    CONSTRAINT uk_idempotency_idem_key UNIQUE (idem_key)
);

-- Additional indexes for common query patterns
-- TODO (itaseski): Add indexes based on usage patterns

