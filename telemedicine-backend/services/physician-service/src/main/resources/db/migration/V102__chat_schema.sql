-- Clinical chat transcript (PRD §13.3). Retained as clinical data, longitudinal per consultation.
-- The body is stored here; only a PHI-free ChatMessageSent marker is published to Kafka.

CREATE TABLE IF NOT EXISTS chat_message (
    id             VARCHAR(64)  PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    sender_role    VARCHAR(16)  NOT NULL,     -- PHYSICIAN | PATIENT
    sender_ref     VARCHAR(128),              -- pseudonymous actor reference
    body           TEXT         NOT NULL,
    clinical       BOOLEAN      NOT NULL DEFAULT TRUE,
    sent_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_chat_message_appointment ON chat_message (appointment_id, sent_at);
