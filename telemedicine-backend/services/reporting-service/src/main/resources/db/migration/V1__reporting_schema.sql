CREATE TABLE IF NOT EXISTS kpi_events (
    id VARCHAR(64) PRIMARY KEY,
    source_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    appointment_id VARCHAR(128),
    metric_family VARCHAR(32) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    numeric_value DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_kpi_events_source UNIQUE (source_event_id, metric_name)
);

CREATE INDEX IF NOT EXISTS idx_kpi_events_family_recorded
    ON kpi_events (metric_family, recorded_at);

CREATE INDEX IF NOT EXISTS idx_kpi_events_metric_recorded
    ON kpi_events (metric_name, recorded_at);

CREATE TABLE IF NOT EXISTS kpi_snapshots (
    id VARCHAR(64) PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    metric_family VARCHAR(32) NOT NULL,
    metric_name VARCHAR(128) NOT NULL,
    aggregate_value DOUBLE PRECISION,
    sample_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_kpi_snapshots UNIQUE (snapshot_date, metric_family, metric_name)
);

CREATE TABLE IF NOT EXISTS journey_milestones (
    appointment_id VARCHAR(128) NOT NULL,
    milestone VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (appointment_id, milestone)
);

CREATE TABLE IF NOT EXISTS report_exports (
    id VARCHAR(64) PRIMARY KEY,
    format VARCHAR(16) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    download_url TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
