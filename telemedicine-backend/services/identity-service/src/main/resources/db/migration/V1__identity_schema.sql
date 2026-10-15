CREATE TABLE IF NOT EXISTS identity_verifications (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    patient_ref VARCHAR(128) NOT NULL,
    method VARCHAR(64) NOT NULL,
    validated BOOLEAN NOT NULL,
    validated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_identity_verification UNIQUE (appointment_id, method)
);

CREATE TABLE IF NOT EXISTS fonasa_challenges (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    patient_ref VARCHAR(128) NOT NULL,
    masked_phone VARCHAR(32),
    rut_encrypted TEXT,
    phone_encrypted TEXT,
    attempts INT NOT NULL DEFAULT 0,
    lockout_until TIMESTAMPTZ,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fonasa_challenges_appointment ON fonasa_challenges (appointment_id);
