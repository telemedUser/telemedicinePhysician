CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    diagnosis_encrypted TEXT,
    storage_key VARCHAR(512) NOT NULL,
    bucket VARCHAR(128) NOT NULL,
    physician_ref VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_documents_appointment ON documents (appointment_id);

CREATE TABLE IF NOT EXISTS prescriptions (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    external_prescription_id VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    medications_encrypted TEXT,
    storage_key VARCHAR(512) NOT NULL,
    bucket VARCHAR(128) NOT NULL,
    physician_ref VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_appointment ON prescriptions (appointment_id);

CREATE TABLE IF NOT EXISTS medical_leaves (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    external_certificate_id VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    duration_days INT NOT NULL,
    diagnosis_encrypted TEXT,
    storage_key VARCHAR(512) NOT NULL,
    bucket VARCHAR(128) NOT NULL,
    physician_ref VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_medical_leaves_appointment ON medical_leaves (appointment_id);

CREATE TABLE IF NOT EXISTS download_tokens (
    id VARCHAR(64) PRIMARY KEY,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    redeemed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_download_tokens_resource ON download_tokens (resource_type, resource_id);
