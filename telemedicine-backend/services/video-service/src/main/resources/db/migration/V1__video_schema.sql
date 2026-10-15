CREATE TABLE IF NOT EXISTS video_sessions (
    id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    provider_session_id VARCHAR(256) NOT NULL,
    recording_policy VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    end_reason VARCHAR(64),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_video_sessions_appointment ON video_sessions (appointment_id);
CREATE INDEX IF NOT EXISTS idx_video_sessions_expires
    ON video_sessions (expires_at)
    WHERE status <> 'ENDED';

CREATE TABLE IF NOT EXISTS session_tokens (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES video_sessions (id),
    role VARCHAR(32) NOT NULL,
    provider_token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_session_tokens_session ON session_tokens (session_id);

CREATE TABLE IF NOT EXISTS chat_threads (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES video_sessions (id),
    appointment_id VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_threads_session ON chat_threads (session_id);

CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(64) PRIMARY KEY,
    thread_id VARCHAR(64) NOT NULL REFERENCES chat_threads (id),
    session_id VARCHAR(64) NOT NULL,
    appointment_id VARCHAR(128) NOT NULL,
    sender_role VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    last_modified_by VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages (session_id, sent_at);
