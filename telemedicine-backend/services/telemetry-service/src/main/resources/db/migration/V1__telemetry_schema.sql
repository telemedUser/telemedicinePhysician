CREATE TABLE IF NOT EXISTS measurements (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    session_id VARCHAR(128),
    metric_type VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    raw_payload TEXT,
    measured_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_measurements_appointment ON measurements (appointment_id);
CREATE INDEX IF NOT EXISTS idx_measurements_session ON measurements (session_id);
