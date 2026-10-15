package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * An existing appointment moved to a new time.
 *
 * @param appointmentId platform identifier for the appointment
 * @param previousScheduledAt time the appointment held before this change
 * @param scheduledAt new start time
 * @param reasonCode coded reason, never free text
 */
public record AppointmentRescheduled(
        String appointmentId, Instant previousScheduledAt, Instant scheduledAt, String reasonCode)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.APPOINTMENT_RESCHEDULED;
    }
}
