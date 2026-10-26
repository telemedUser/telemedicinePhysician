-- Care plan for the D-08 Review screen, keyed by appointment. Persists the physician's clinical note
-- (history/findings/assessment/plan), the selected diagnoses, prescriptions, ordered diagnostic
-- tests, follow-up, medical leave and the patient-facing summary. Lists are stored as JSON so the
-- shape can evolve without schema churn; this is the physician-side system of record for the plan
-- that will later be surfaced to the patient.

CREATE TABLE IF NOT EXISTS care_plan (
    appointment_id        VARCHAR(128) PRIMARY KEY,
    provider_id           VARCHAR(128),
    market                VARCHAR(16)  NOT NULL DEFAULT 'IN',

    -- Clinical note (SOAP-ish, four sections).
    note_history          TEXT,
    note_findings         TEXT,
    note_assessment       TEXT,
    note_plan             TEXT,

    -- Structured lists (JSON).
    diagnoses             TEXT,   -- [{code,title,primary}]
    prescriptions         TEXT,   -- [{medicationId,name,strength,form,dosage,frequency,durationDays,quantity,instructions}]
    diagnostic_tests      TEXT,   -- [{testId,name,category,specimen,note}]

    -- Follow-up.
    follow_up_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    follow_up_mode        VARCHAR(32),   -- VIDEO | AUDIO | IN_PERSON
    follow_up_in_days     INT,
    follow_up_date        VARCHAR(32),
    follow_up_instructions TEXT,

    -- Medical leave.
    leave_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    leave_reason          VARCHAR(128),
    leave_start_date      VARCHAR(32),
    leave_end_date        VARCHAR(32),

    -- Patient-facing summary.
    summary_text          TEXT,
    summary_instructions  TEXT,

    status                VARCHAR(16) NOT NULL DEFAULT 'DRAFT',  -- DRAFT | APPROVED
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_care_plan_provider ON care_plan (provider_id, market);
