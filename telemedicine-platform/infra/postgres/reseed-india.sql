-- Re-seed the live physician DB with Indian demo data (market IN).
-- Safe to re-run: clears provider + worklist demo rows first.

DELETE FROM provider_metrics;
DELETE FROM availability;
DELETE FROM provider_credential;
DELETE FROM provider_language;
DELETE FROM provider_mode;
DELETE FROM provider_specialty;
DELETE FROM provider;
DELETE FROM worklist_item;

INSERT INTO provider (id, market, display_name, status, source_ref, last_refreshed_at, staleness_seconds, created_at, version) VALUES
  ('prov-aks', 'IN', 'Dr. Arjun Khanna',  'ACTIVE',    'his://practitioner/884217', NOW(), 900, NOW(), 0),
  ('prov-mnp', 'IN', 'Dr. Meera Nair',    'ACTIVE',    'his://practitioner/771004', NOW(), 900, NOW(), 0),
  ('prov-sgp', 'IN', 'Dr. Sanjay Gupta',  'ON_LEAVE',  'his://practitioner/2290',   NOW() - INTERVAL '60 minutes', 900, NOW(), 0),
  ('prov-ard', 'IN', 'Dr. Ananya Rao',    'ACTIVE',    'his://practitioner/660912', NOW(), 900, NOW(), 0),
  ('prov-ism', 'IN', 'Dr. Imran Shaikh',  'SUSPENDED', 'his://practitioner/1180',   NOW() - INTERVAL '240 minutes', 900, NOW(), 0);

INSERT INTO provider_specialty (provider_id, specialty) VALUES
  ('prov-aks', 'General Medicine'), ('prov-mnp', 'Pediatrics'), ('prov-sgp', 'Cardiology'),
  ('prov-ard', 'Dermatology'), ('prov-ism', 'Internal Medicine');

INSERT INTO provider_mode (provider_id, mode) VALUES
  ('prov-aks', 'video'), ('prov-aks', 'audio'), ('prov-aks', 'chat'),
  ('prov-mnp', 'video'), ('prov-mnp', 'audio'),
  ('prov-sgp', 'video'),
  ('prov-ard', 'video'), ('prov-ard', 'chat'),
  ('prov-ism', 'video'), ('prov-ism', 'audio');

INSERT INTO provider_language (provider_id, language) VALUES
  ('prov-aks', 'en'), ('prov-aks', 'hi'),
  ('prov-mnp', 'en'), ('prov-mnp', 'ml'),
  ('prov-sgp', 'en'), ('prov-sgp', 'hi'),
  ('prov-ard', 'en'), ('prov-ard', 'kn'),
  ('prov-ism', 'en'), ('prov-ism', 'hi');

INSERT INTO provider_credential (provider_id, type, number, valid_until, verified) VALUES
  ('prov-aks', 'NMC registration',          'NMC-KA-884217', DATE '2028-03-31', TRUE),
  ('prov-aks', 'Specialty certificate (MD)','MD-GM-5521',    DATE '2027-11-30', TRUE),
  ('prov-mnp', 'NMC registration',          'NMC-KL-771004', DATE '2029-06-30', TRUE),
  ('prov-sgp', 'Specialty certificate (DM)','DM-CAR-2290',   DATE '2026-01-31', TRUE),
  ('prov-ard', 'NMC registration',          'NMC-KA-660912', DATE '2028-09-30', TRUE),
  ('prov-ism', 'Specialty certificate (MD)','MD-MI-1180',    DATE '2024-12-31', FALSE);

INSERT INTO availability (provider_id, market, day_of_week, start_time, end_time, mode, max_concurrent)
SELECT 'prov-aks', 'IN', d, TIME '09:00', TIME '18:00', 'video', 1 FROM generate_series(1, 5) AS d;
INSERT INTO availability (provider_id, market, day_of_week, start_time, end_time, mode, max_concurrent)
SELECT 'prov-mnp', 'IN', d, TIME '09:00', TIME '18:00', 'video', 2 FROM generate_series(1, 5) AS d;
INSERT INTO availability (provider_id, market, day_of_week, start_time, end_time, mode, max_concurrent)
SELECT 'prov-ard', 'IN', d, TIME '09:00', TIME '18:00', 'video', 1 FROM generate_series(1, 3) AS d;

INSERT INTO provider_metrics (provider_id, utilization, avg_duration_seconds, avg_documentation_seconds, no_show_rate, technical_failure_rate) VALUES
  ('prov-aks', 0.78, 1020, 240, 0.06, 0.02),
  ('prov-mnp', 0.84, 1260, 300, 0.04, 0.03),
  ('prov-sgp', 0.00, 1560, 360, 0.05, 0.02),
  ('prov-ard', 0.71, 900,  180, 0.08, 0.01),
  ('prov-ism', 0.00, 1140, 420, 0.11, 0.04);

-- Active worklist for the dev physician (market IN)
INSERT INTO worklist_item
  (appointment_id, provider_id, market, patient_ref, mode, entered_at, readiness,
   consultation_status, technical_status, telemetry_available, flags,
   red_flag_acknowledged_at, red_flag_acknowledged_by, updated_at)
VALUES
  ('AP-20931', 'dev-physician', 'IN', 'Priya Sharma', 'video',
   NOW() - INTERVAL '9 minutes', 'READY', 'CHECKED_IN', 'OK', TRUE, '["FOLLOW_UP"]', NULL, NULL, NOW()),
  ('AP-20944', 'dev-physician', 'IN', 'Rohan Verma', 'video',
   NOW() - INTERVAL '6 minutes', 'READY', 'CHECKED_IN', 'OK', FALSE, '["FIRST_VISIT","RED_FLAG"]', NULL, NULL, NOW()),
  ('AP-20950', 'dev-physician', 'IN', 'Lakshmi Iyer', 'audio',
   NOW() - INTERVAL '3 minutes', 'READY', 'CHECKED_IN', 'OK', FALSE, '["GUEST"]', NULL, NULL, NOW()),
  ('AP-20962', 'dev-physician', 'IN', 'Aarav Nair (minor)', 'video',
   NOW() - INTERVAL '2 minutes', 'IN_PROGRESS', 'CHECKED_IN', 'OK', FALSE, '["FIRST_VISIT"]', NULL, NULL, NOW()),
  ('AP-20977', 'dev-physician', 'IN', 'Vikram Reddy', 'video',
   NOW() - INTERVAL '1 minutes', 'NOT_READY', 'CHECKED_IN', 'IMPAIRED', FALSE, '["FIRST_VISIT"]', NULL, NULL, NOW());
