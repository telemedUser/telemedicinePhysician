-- Expand audit trail for tamper-evident hash chaining and compliance queries.

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS actor_ip VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512),
    ADD COLUMN IF NOT EXISTS reason VARCHAR(512),
    ADD COLUMN IF NOT EXISTS sequence BIGINT,
    ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'audit_events' AND column_name = 'hash_chain'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'audit_events' AND column_name = 'hash'
    ) THEN
        ALTER TABLE audit_events RENAME COLUMN hash_chain TO hash;
    END IF;
END
$$;

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS hash VARCHAR(128);

WITH ordered AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY occurred_at, id) AS row_num,
           LAG(hash) OVER (ORDER BY occurred_at, id) AS prev_hash
    FROM audit_events
)
UPDATE audit_events AS ae
SET sequence = ordered.row_num,
    previous_hash = COALESCE(ordered.prev_hash, ''),
    hash = COALESCE(ae.hash, '')
FROM ordered
WHERE ae.id = ordered.id
  AND ae.sequence IS NULL;

UPDATE audit_events SET sequence = 1 WHERE sequence IS NULL;
UPDATE audit_events SET previous_hash = '' WHERE previous_hash IS NULL;
UPDATE audit_events SET hash = '' WHERE hash IS NULL;

ALTER TABLE audit_events
    ALTER COLUMN sequence SET NOT NULL,
    ALTER COLUMN previous_hash SET NOT NULL,
    ALTER COLUMN hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_events_sequence ON audit_events (sequence);

ALTER TABLE consent_ledger
    ADD COLUMN IF NOT EXISTS subject_ref VARCHAR(128),
    ADD COLUMN IF NOT EXISTS scope VARCHAR(128),
    ADD COLUMN IF NOT EXISTS withdrawn_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_consent_ledger_entry
    ON consent_ledger (appointment_id, consent_type, version, granted);

CREATE TABLE IF NOT EXISTS audit_chain_head (
    id VARCHAR(32) PRIMARY KEY,
    last_sequence BIGINT NOT NULL,
    last_hash VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO audit_chain_head (id, last_sequence, last_hash)
VALUES (
    'GLOBAL',
    COALESCE((SELECT MAX(sequence) FROM audit_events), 0),
    COALESCE((SELECT hash FROM audit_events ORDER BY sequence DESC LIMIT 1), '')
)
ON CONFLICT (id) DO NOTHING;
