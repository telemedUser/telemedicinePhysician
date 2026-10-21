-- Patient-detail read model for the physician "review & join" page (prototype D-03).
-- Populated by WorklistProjectionListener from the ConsultationContextPublished event that the
-- patient-booking-service emits when a patient is ready. Like the worklist, this is a plain
-- (non-BaseEntity) projection keyed by appointment id, holding only what the event carries.
-- The variable-length clinical detail travels as a single JSON document (context_json).

CREATE TABLE IF NOT EXISTS patient_detail (
    appointment_id          VARCHAR(128) PRIMARY KEY,
    market                  VARCHAR(16)  NOT NULL DEFAULT 'IN',
    patient_display         VARCHAR(256),
    age_years               INT          NOT NULL DEFAULT 0,
    sex_code                VARCHAR(16),
    city                    VARCHAR(128),
    insurance_scheme        VARCHAR(128),
    national_id_masked      VARCHAR(64),
    modality                VARCHAR(16),
    visit_type              VARCHAR(32),
    scheduled_at            TIMESTAMPTZ,
    severity_score          INT          NOT NULL DEFAULT 0,
    symptom_duration_code   VARCHAR(32),
    symptom_count           INT          NOT NULL DEFAULT 0,
    attachment_count        INT          NOT NULL DEFAULT 0,
    red_flag                BOOLEAN      NOT NULL DEFAULT FALSE,
    consultation_fee_minor  BIGINT       NOT NULL DEFAULT 0,
    voucher_minor           BIGINT       NOT NULL DEFAULT 0,
    amount_paid_minor       BIGINT       NOT NULL DEFAULT 0,
    currency                VARCHAR(8)   NOT NULL DEFAULT 'INR',
    payment_status          VARCHAR(32),
    context_json            TEXT,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
