CREATE TABLE IF NOT EXISTS platform_config (
    config_key VARCHAR(128) PRIMARY KEY,
    config_value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO platform_config (config_key, config_value) VALUES
    ('video.provider', 'simulator'),
    ('chat.provider', 'simulator'),
    ('telemetry.enabled', 'true'),
    ('telemetry.activation.rules', '{"appointmentTypes":[],"specialties":[]}'),
    ('notification.email.enabled', 'true'),
    ('notification.sms.enabled', 'true'),
    ('notification.whatsapp.enabled', 'false'),
    ('notification.push.enabled', 'true'),
    ('mfe.templateMfe.enabled', 'true'),
    ('mfe.mfePatientConsent.enabled', 'true'),
    ('mfe.mfePatientIdentity.enabled', 'true'),
    ('mfe.mfePatientWaitingRoom.enabled', 'true'),
    ('mfe.mfePatientConsultation.enabled', 'true'),
    ('mfe.mfePatientDocuments.enabled', 'true'),
    ('mfe.mfePatientSurvey.enabled', 'true')
ON CONFLICT (config_key) DO NOTHING;
