CREATE TABLE IF NOT EXISTS survey_definitions (
    id VARCHAR(64) PRIMARY KEY,
    survey_version INT NOT NULL,
    name VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    questions_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_survey_definition_version UNIQUE (name, survey_version)
);

CREATE TABLE IF NOT EXISTS survey_requests (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL UNIQUE,
    survey_definition_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_survey_requests_status ON survey_requests (status);

CREATE TABLE IF NOT EXISTS survey_responses (
    id VARCHAR(64) PRIMARY KEY,
    survey_request_id VARCHAR(64) NOT NULL,
    appointment_id VARCHAR(128) NOT NULL UNIQUE,
    score INT NOT NULL,
    comment_encrypted TEXT,
    completed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO survey_definitions (id, survey_version, name, active, questions_json, created_at, updated_at)
VALUES (
    'nps-v1',
    1,
    'post-consultation-nps',
    TRUE,
    '[{"id":"nps","type":"nps","prompt":"How likely are you to recommend this service?"}]',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;
