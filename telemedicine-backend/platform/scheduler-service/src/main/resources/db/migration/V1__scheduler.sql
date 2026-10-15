CREATE TABLE IF NOT EXISTS job_definitions (
    job_key VARCHAR(128) PRIMARY KEY,
    description TEXT NOT NULL,
    cron_expression VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    target_service VARCHAR(128),
    target_path VARCHAR(256),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS job_runs (
    id VARCHAR(64) PRIMARY KEY,
    job_key VARCHAR(128) NOT NULL REFERENCES job_definitions (job_key),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    message TEXT,
    trigger_type VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_runs_job_key_started ON job_runs (job_key, started_at DESC);

INSERT INTO job_definitions (job_key, description, cron_expression, enabled, target_service, target_path)
VALUES
    (
        'appointment-reminders',
        'Dispatch appointment reminder notifications',
        '0 */15 * * * *',
        TRUE,
        'notification-service',
        '/api/v1/internal/jobs/appointment-reminders/run'
    ),
    (
        'waiting-room-timeout',
        'Expire patients who exceed waiting-room limits',
        '0 */5 * * * *',
        TRUE,
        'waiting-room-service',
        '/api/v1/internal/jobs/waiting-room-timeout/run'
    ),
    (
        'no-show-detection',
        'Mark appointments as no-show when thresholds are exceeded',
        '0 */5 * * * *',
        TRUE,
        'waiting-room-service',
        '/api/v1/internal/jobs/no-show-detection/run'
    ),
    (
        'session-expiry-cleanup',
        'Close expired video sessions and release provider resources',
        '0 */5 * * * *',
        TRUE,
        'video-service',
        '/api/v1/internal/jobs/session-expiry-cleanup/run'
    ),
    (
        'survey-dispatch',
        'Dispatch post-consultation satisfaction surveys',
        '0 0 * * * *',
        TRUE,
        'survey-service',
        '/api/v1/internal/jobs/survey-dispatch/run'
    ),
    (
        'report-generation',
        'Generate scheduled KPI and audit reports',
        '0 0 2 * * *',
        TRUE,
        'reporting-service',
        '/api/v1/internal/jobs/report-generation/run'
    ),
    (
        'audit-retention',
        'Archive audit data and apply retention policy',
        '0 0 3 * * *',
        TRUE,
        'audit-service',
        '/api/v1/audit/retention/run'
    ),
    (
        'config-cache-refresh',
        'Tick metric for configuration cache refresh consumers',
        '0 */1 * * * *',
        TRUE,
        NULL,
        NULL
    )
ON CONFLICT (job_key) DO NOTHING;
