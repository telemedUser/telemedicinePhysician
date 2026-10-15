CREATE TABLE IF NOT EXISTS config_revisions (
    id VARCHAR(64) PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL,
    previous_value_hash VARCHAR(64),
    new_value_hash VARCHAR(64) NOT NULL,
    changed_by VARCHAR(128) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_config_revisions_key_changed
    ON config_revisions (config_key, changed_at DESC);

CREATE TABLE IF NOT EXISTS feature_flags (
    flag_key VARCHAR(128) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    payload_json TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
