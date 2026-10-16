-- Re-scope the demo worklist rows to real physician ids (matching the patient-facing doctor ids
-- doc-khanna / doc-nair / doc-reddy / doc-iyer) so that a physician who signs in as one of those
-- doctors sees only their own patients. Previously every demo row was owned by 'dev-physician'.
-- Live patient bookings now also route by doctor id (AppointmentCreated.physicianRef = doctorId).

UPDATE worklist_item SET provider_id = 'doc-khanna' WHERE appointment_id IN ('TM-DEMO01', 'TM-DEMO05');
UPDATE worklist_item SET provider_id = 'doc-nair'   WHERE appointment_id = 'TM-DEMO02';
UPDATE worklist_item SET provider_id = 'doc-reddy'  WHERE appointment_id = 'TM-DEMO03';
UPDATE worklist_item SET provider_id = 'doc-iyer'   WHERE appointment_id = 'TM-DEMO04';
UPDATE worklist_item SET provider_id = 'doc-khanna' WHERE appointment_id = 'TM-DEMO06';
