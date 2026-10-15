CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(64) PRIMARY KEY,
    actor_id VARCHAR(128) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(128) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    hash_chain VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS consent_ledger (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    consent_type VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    granted BOOLEAN NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
