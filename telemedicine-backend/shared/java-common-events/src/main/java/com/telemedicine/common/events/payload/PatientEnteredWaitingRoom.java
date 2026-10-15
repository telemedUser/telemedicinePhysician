package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The patient reached the virtual waiting room. Start of the waiting-time KPI.
 *
 * @param appointmentId appointment being waited for
 * @param patientRef pseudonymous patient reference
 * @param queuePosition position at the moment of entry, one-based
 * @param enteredAt when the patient entered
 */
public record PatientEnteredWaitingRoom(
        String appointmentId, String patientRef, int queuePosition, Instant enteredAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.PATIENT_ENTERED_WAITING_ROOM;
    }
}
