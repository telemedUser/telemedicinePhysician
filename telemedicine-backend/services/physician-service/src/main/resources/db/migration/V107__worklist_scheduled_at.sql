-- Add the patient's scheduled consultation time to the worklist read model. This is the slot time
-- the patient booked (AppointmentCreated.scheduledAt from the patient service), distinct from
-- entered_at (when they reached the waiting room). The Home "Virtual clinic" table shows this as the
-- per-patient "Time" column.

ALTER TABLE worklist_item ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMPTZ;

-- Backfill the demo worklist rows with staggered afternoon slots so the dashboard reads like a real
-- clinic until live bookings (which carry their own scheduledAt) replace them.
UPDATE worklist_item SET scheduled_at = DATE_TRUNC('day', NOW()) + TIME '16:20' WHERE appointment_id = 'TM-DEMO01' AND scheduled_at IS NULL;
UPDATE worklist_item SET scheduled_at = DATE_TRUNC('day', NOW()) + TIME '16:40' WHERE appointment_id = 'TM-DEMO05' AND scheduled_at IS NULL;
UPDATE worklist_item SET scheduled_at = DATE_TRUNC('day', NOW()) + TIME '17:00' WHERE appointment_id = 'TM-DEMO06' AND scheduled_at IS NULL;
