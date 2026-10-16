-- Replace the earlier placeholder worklist seed with a demo set that mirrors the reference
-- worklist composition (Completed / In consultation / Ready / Not arrived / Not arrived + Red flag),
-- using Indian display names as the pseudonymous patient_ref so the UI reads naturally. Upserts so
-- it also corrects rows a prior seed created. Owned by the dev physician, market IN.

DELETE FROM worklist_item WHERE appointment_id IN
  ('TM-WL0001','TM-WL0002','TM-WL0003','TM-WL0004',
   'TM-DEMO01','TM-DEMO02','TM-DEMO03','TM-DEMO04','TM-DEMO05','TM-DEMO06');

INSERT INTO worklist_item
    (appointment_id, provider_id, market, patient_ref, mode, entered_at, readiness,
     consultation_status, technical_status, telemetry_available, flags, updated_at)
VALUES
    -- Completed consultation.
    ('TM-DEMO01', 'dev-physician', 'IN', 'Aarav Sharma', 'video',
     NOW() - INTERVAL '55 minutes', 'READY', 'CLOSED', 'OK', TRUE, '[]', NOW()),
    -- In consultation now.
    ('TM-DEMO02', 'dev-physician', 'IN', 'Priya Nair', 'video',
     NOW() - INTERVAL '20 minutes', 'READY', 'OPEN_DOCTOR', 'OK', TRUE, '[]', NOW()),
    -- Ready to see (active row, highlighted).
    ('TM-DEMO03', 'dev-physician', 'IN', 'Vikram Reddy', 'audio',
     NOW() - INTERVAL '8 minutes', 'READY', 'CHECKED_IN', 'OK', TRUE, '["FIRST_VISIT"]', NOW()),
    -- Not arrived yet.
    ('TM-DEMO04', 'dev-physician', 'IN', 'Meera Iyer', 'video',
     NULL, 'NOT_READY', 'SCHEDULED', 'OK', FALSE, '[]', NOW()),
    -- Not arrived yet.
    ('TM-DEMO05', 'dev-physician', 'IN', 'Rohan Verma', 'video',
     NULL, 'NOT_READY', 'SCHEDULED', 'OK', FALSE, '["FOLLOW_UP"]', NOW()),
    -- Not arrived, with a red flag.
    ('TM-DEMO06', 'dev-physician', 'IN', 'Sanjay Gupta', 'video',
     NULL, 'NOT_READY', 'SCHEDULED', 'OK', FALSE, '["RED_FLAG"]', NOW());
