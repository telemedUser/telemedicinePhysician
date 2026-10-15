-- Demo worklist rows for the dev physician (the dev security principal is "dev-physician").
-- Normally these are projected from waiting-room Kafka events; seeded here for the review flow.
DELETE FROM worklist_item;

INSERT INTO worklist_item
  (appointment_id, provider_id, market, patient_ref, mode, entered_at, readiness,
   consultation_status, technical_status, telemetry_available, flags,
   red_flag_acknowledged_at, red_flag_acknowledged_by, updated_at)
VALUES
  ('AH-20931', 'dev-physician', 'CL', 'Valentina Rojas Méndez', 'video',
   NOW() - INTERVAL '9 minutes', 'READY', 'CHECKED_IN', 'OK', TRUE,
   '["FOLLOW_UP"]', NULL, NULL, NOW()),

  ('AH-20944', 'dev-physician', 'CL', 'Sebastián Herrera Pino', 'video',
   NOW() - INTERVAL '6 minutes', 'READY', 'CHECKED_IN', 'OK', FALSE,
   '["FIRST_VISIT","RED_FLAG"]', NULL, NULL, NOW()),

  ('AH-20950', 'dev-physician', 'CL', 'Carolina Soto Vega', 'audio',
   NOW() - INTERVAL '3 minutes', 'READY', 'CHECKED_IN', 'OK', FALSE,
   '["GUEST"]', NULL, NULL, NOW()),

  ('AH-20962', 'dev-physician', 'CL', 'Emilia Araya (minor)', 'video',
   NOW() - INTERVAL '2 minutes', 'IN_PROGRESS', 'CHECKED_IN', 'OK', FALSE,
   '["FIRST_VISIT"]', NULL, NULL, NOW()),

  ('AH-20977', 'dev-physician', 'CL', 'Diego Núñez Fuentes', 'video',
   NOW() - INTERVAL '1 minutes', 'NOT_READY', 'CHECKED_IN', 'IMPAIRED', FALSE,
   '["FIRST_VISIT"]', NULL, NULL, NOW());
