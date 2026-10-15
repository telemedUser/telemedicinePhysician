CREATE TABLE IF NOT EXISTS session_health (
    session_id VARCHAR(128) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    provider VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    quality_score INT,
    last_quality_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_session_health_status ON session_health (status);
CREATE INDEX IF NOT EXISTS idx_session_health_appointment ON session_health (appointment_id);

CREATE TABLE IF NOT EXISTS alert_rules (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    metric VARCHAR(128) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    comparison VARCHAR(8) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS alerts (
    id VARCHAR(64) PRIMARY KEY,
    rule_id VARCHAR(64) NOT NULL REFERENCES alert_rules (id),
    session_id VARCHAR(128),
    appointment_id VARCHAR(128),
    message TEXT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alerts_status ON alerts (status);

CREATE TABLE IF NOT EXISTS slo_windows (
    id VARCHAR(64) PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    total_requests BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    burn_rate DOUBLE PRECISION NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_slo_windows_end ON slo_windows (window_end DESC);

INSERT INTO alert_rules (id, name, metric, threshold, comparison, severity, enabled)
VALUES
    ('rule-quality', 'connection-quality-degraded', 'quality_score', 50, 'LT', 'WARNING', TRUE),
    ('rule-error-rate', 'elevated-error-rate', 'error_rate', 0.05, 'GT', 'CRITICAL', TRUE)
ON CONFLICT (id) DO NOTHING;
