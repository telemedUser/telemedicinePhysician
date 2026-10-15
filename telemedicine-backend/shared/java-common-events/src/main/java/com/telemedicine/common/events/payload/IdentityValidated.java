package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The patient's identity was confirmed, for example against FONASA.
 *
 * @param appointmentId appointment the check belongs to
 * @param patientRef pseudonymous patient reference; the national identifier is never published
 * @param method coded verification method, for example {@code FONASA_2FA}
 * @param validatedAt when verification succeeded
 */
public record IdentityValidated(
        String appointmentId, String patientRef, String method, Instant validatedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.IDENTITY_VALIDATED;
    }
}
