CREATE TABLE IF NOT EXISTS templates (
    id VARCHAR(64) PRIMARY KEY,
    template_key VARCHAR(128) NOT NULL,
    locale VARCHAR(8) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    subject VARCHAR(256),
    body TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_template UNIQUE (template_key, locale, channel)
);

CREATE TABLE IF NOT EXISTS notification_log (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128),
    channel VARCHAR(32) NOT NULL,
    template_key VARCHAR(128) NOT NULL,
    recipient_ref VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(64),
    provider_message_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_log_appointment ON notification_log (appointment_id);
