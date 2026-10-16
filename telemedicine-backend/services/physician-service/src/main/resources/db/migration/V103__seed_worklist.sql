-- Seed demo worklist items for the dev physician (market IN) so the physician worklist has data
-- immediately on first boot, without waiting for a live patient booking event. These mirror the
-- shapes the AppointmentCreated / PatientEnteredWaitingRoom projections produce. The provider_id
-- 'dev-physician' matches the dev security principal the standalone physician app authenticates as.
-- Idempotent via ON CONFLICT so re-running against an existing DB is harmless.

INSERT INTO worklist_item
    (appointment_id, provider_id, market, patient_ref, mode, entered_at, readiness,
     consultation_status, technical_status, telemetry_available, flags, updated_at)
VALUES
    ('TM-WL0001', 'dev-physician', 'IN', 'patient-priya', 'video',
     NOW() - INTERVAL '6 minutes', 'READY', 'CHECKED_IN', 'OK', TRUE,
     '["FIRST_VISIT"]', NOW()),
    ('TM-WL0002', 'dev-physician', 'IN', 'patient-rohan', 'video',
     NOW() - INTERVAL '12 minutes', 'READY', 'CHECKED_IN', 'OK', TRUE,
     '["FOLLOW_UP","RED_FLAG"]', NOW()),
    ('TM-WL0003', 'dev-physician', 'IN', 'patient-aarav', 'video',
     NOW() - INTERVAL '2 minutes', 'IN_PROGRESS', 'CHECKED_IN', 'OK', FALSE,
     '["FIRST_VISIT"]', NOW()),
    ('TM-WL0004', 'dev-physician', 'IN', 'patient-meera', 'audio',
     NULL, 'NOT_READY', 'SCHEDULED', 'OK', FALSE,
     '["FOLLOW_UP"]', NOW())
ON CONFLICT (appointment_id) DO NOTHING;
