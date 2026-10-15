-- Physician service schema (PRD §21 Provider & Physician Management, §12.3 Worklist, §14.2 Closure).
-- Every domain row carries `market` (TM-MKT-002). No clinical bodies are stored, only references
-- and pseudonymous actor/patient refs (TM-SEC-014/019).
--
-- Provider-family tables use the shared BaseEntity shape: string `id` PK plus auditing columns
-- (created_at / updated_at / created_by / last_modified_by / version). Child tables use a surrogate
-- BIGSERIAL id. The worklist and closure read models are plain (non-BaseEntity) tables keyed by
-- appointment id.

CREATE TABLE IF NOT EXISTS provider (
    id                 VARCHAR(128) PRIMARY KEY,          -- HIS id; projection key
    market             VARCHAR(16)  NOT NULL,
    display_name       VARCHAR(256) NOT NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE|SUSPENDED|ON_LEAVE|OFFBOARDED
    routing_attributes TEXT,                              -- years experience, rating, education (JSON)
    source_ref         VARCHAR(256),                      -- HIS source reference (TM-ARC-020)
    last_refreshed_at  TIMESTAMPTZ,
    staleness_seconds  INT          NOT NULL DEFAULT 900,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ,
    created_by         VARCHAR(128),
    last_modified_by   VARCHAR(128),
    version            BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_provider_market_status ON provider (market, status);

CREATE TABLE IF NOT EXISTS provider_specialty (
    id            BIGSERIAL    PRIMARY KEY,
    provider_id   VARCHAR(128) NOT NULL REFERENCES provider (id) ON DELETE CASCADE,
    specialty     VARCHAR(128) NOT NULL,
    sub_specialty VARCHAR(128)
);
CREATE INDEX IF NOT EXISTS idx_specialty_provider ON provider_specialty (provider_id);

CREATE TABLE IF NOT EXISTS provider_mode (
    id          BIGSERIAL    PRIMARY KEY,
    provider_id VARCHAR(128) NOT NULL REFERENCES provider (id) ON DELETE CASCADE,
    mode        VARCHAR(16)  NOT NULL                      -- video|audio|chat
);
CREATE INDEX IF NOT EXISTS idx_mode_provider ON provider_mode (provider_id);

CREATE TABLE IF NOT EXISTS provider_language (
    id          BIGSERIAL    PRIMARY KEY,
    provider_id VARCHAR(128) NOT NULL REFERENCES provider (id) ON DELETE CASCADE,
    language    VARCHAR(16)  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_language_provider ON provider_language (provider_id);

-- Credential expiry gate (TM-PRV-006). The platform flags expiry; it is NOT the verification SoR.
CREATE TABLE IF NOT EXISTS provider_credential (
    id          BIGSERIAL    PRIMARY KEY,
    provider_id VARCHAR(128) NOT NULL REFERENCES provider (id) ON DELETE CASCADE,
    type        VARCHAR(128) NOT NULL,
    number      VARCHAR(128),
    valid_from  DATE,
    valid_until DATE,
    verified    BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_credential_provider ON provider_credential (provider_id);

-- Availability & capacity (system of record, platform-native).
CREATE TABLE IF NOT EXISTS availability (
    id             BIGSERIAL    PRIMARY KEY,
    provider_id    VARCHAR(128) NOT NULL REFERENCES provider (id) ON DELETE CASCADE,
    market         VARCHAR(16)  NOT NULL,
    day_of_week    SMALLINT     NOT NULL,                  -- 1=Mon .. 7=Sun
    start_time     TIME         NOT NULL,
    end_time       TIME         NOT NULL,
    mode           VARCHAR(16)  NOT NULL,
    max_concurrent INT          NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_availability_provider ON availability (provider_id);

CREATE TABLE IF NOT EXISTS provider_metrics (
    provider_id               VARCHAR(128) PRIMARY KEY REFERENCES provider (id) ON DELETE CASCADE,
    utilization               DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_duration_seconds      BIGINT NOT NULL DEFAULT 0,
    avg_documentation_seconds BIGINT NOT NULL DEFAULT 0,
    no_show_rate              DOUBLE PRECISION NOT NULL DEFAULT 0,
    technical_failure_rate    DOUBLE PRECISION NOT NULL DEFAULT 0
);

-- Worklist read model (derived from events). Rebuilt from the event stream; API writes limited
-- to red-flag acknowledgement and operator force-transition.
CREATE TABLE IF NOT EXISTS worklist_item (
    appointment_id          VARCHAR(128) PRIMARY KEY,
    provider_id             VARCHAR(128),
    market                  VARCHAR(16)  NOT NULL,
    patient_ref             VARCHAR(128),                  -- pseudonymous
    mode                    VARCHAR(16),
    entered_at              TIMESTAMPTZ,
    readiness               VARCHAR(16)  NOT NULL DEFAULT 'NOT_READY', -- NOT_READY|IN_PROGRESS|READY
    consultation_status     VARCHAR(32)  NOT NULL DEFAULT 'CHECKED_IN',
    technical_status        VARCHAR(16)  NOT NULL DEFAULT 'OK',        -- OK|DEGRADED|IMPAIRED
    telemetry_available     BOOLEAN      NOT NULL DEFAULT FALSE,
    flags                   TEXT,                          -- JSON array: FIRST_VISIT|FOLLOW_UP|GUEST|RED_FLAG
    red_flag_acknowledged_at TIMESTAMPTZ,
    red_flag_acknowledged_by VARCHAR(128),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_worklist_provider ON worklist_item (provider_id, market);

-- Consultation closure fact (system of record for the closure; documents stay in their SoR).
CREATE TABLE IF NOT EXISTS consultation_closure (
    appointment_id    VARCHAR(128) PRIMARY KEY,
    provider_id       VARCHAR(128),
    market            VARCHAR(16)  NOT NULL,
    started_at        TIMESTAMPTZ,
    closed_at         TIMESTAMPTZ,
    completion_set    TEXT,                                -- JSON {note,diagnosis,prescription,...}
    minimum_satisfied BOOLEAN      NOT NULL DEFAULT FALSE,
    exception_reason  VARCHAR(512),
    document_refs     TEXT,                                -- JSON array
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_closure_provider ON consultation_closure (provider_id, market);
