CREATE TABLE IF NOT EXISTS appointments (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(128) NOT NULL,
    physician_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    scheduled_at TIMESTAMPTZ,
    payment_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    source VARCHAR(64) NOT NULL DEFAULT 'his',
    consultation_closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments (patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_payment_status ON appointments (payment_status);
