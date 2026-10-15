-- Idempotent safety net for deployments that may have applied partial migrations.
CREATE TABLE IF NOT EXISTS processed_events (
    consumer_group VARCHAR(128) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_group, event_id)
);

CREATE INDEX IF NOT EXISTS idx_processed_events_expires ON processed_events (expires_at);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    fingerprint VARCHAR(128) NOT NULL,
    status INT NOT NULL DEFAULT 0,
    response_body TEXT,
    content_type VARCHAR(128),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_keys (expires_at);

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_error VARCHAR(2048),
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ;
