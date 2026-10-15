package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A telemedicine appointment now exists in the source scheduling system.
 *
 * @param appointmentId platform identifier for the appointment
 * @param patientRef pseudonymous patient reference, never a RUT or a name
 * @param physicianRef pseudonymous physician reference
 * @param scheduledAt agreed start time
 * @param modality for example {@code VIDEO}
 * @param sourceSystem system the appointment originated in, for example {@code HIS}
 */
public record AppointmentCreated(
        String appointmentId,
        String patientRef,
        String physicianRef,
        Instant scheduledAt,
        String modality,
        String sourceSystem)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.APPOINTMENT_CREATED;
    }
}
