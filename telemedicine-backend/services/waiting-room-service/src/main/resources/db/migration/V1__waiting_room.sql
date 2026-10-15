CREATE TABLE IF NOT EXISTS consents (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(128) NOT NULL,
    appointment_id VARCHAR(128) NOT NULL,
    consent_type VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    withdrawn_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_consents_subject_type
    ON consents (subject_id, consent_type, withdrawn_at);

CREATE TABLE IF NOT EXISTS session_links (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    token_hash VARCHAR(256) NOT NULL,
    channel VARCHAR(32),
    expires_at TIMESTAMPTZ NOT NULL,
    redeemed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_session_links_appointment_active
    ON session_links (appointment_id)
    WHERE redeemed_at IS NULL;

CREATE TABLE IF NOT EXISTS device_tests (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL UNIQUE,
    patient_ref VARCHAR(128) NOT NULL,
    camera_ok BOOLEAN NOT NULL,
    microphone_ok BOOLEAN NOT NULL,
    bandwidth_kbps INT NOT NULL,
    passed BOOLEAN NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS queue_entries (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL UNIQUE,
    patient_ref VARCHAR(128) NOT NULL,
    queue_position INT NOT NULL,
    entered_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_queue_entries_entered_at ON queue_entries (entered_at);

CREATE TABLE IF NOT EXISTS appointment_status (
    appointment_id VARCHAR(128) PRIMARY KEY,
    patient_ref VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN_PATIENT',
    identity_validated BOOLEAN NOT NULL DEFAULT FALSE,
    payment_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    checked_in_at TIMESTAMPTZ,
    open_doctor_at TIMESTAMPTZ,
    no_show_at TIMESTAMPTZ,
    timed_out_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_appointment_status_status ON appointment_status (status);
