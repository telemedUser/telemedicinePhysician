package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * An identity check did not pass.
 *
 * <p>Repeated failures matter to fraud monitoring, so the attempt number travels with the event.
 *
 * @param appointmentId appointment the check belongs to
 * @param patientRef pseudonymous patient reference
 * @param method coded verification method
 * @param reasonCode coded failure reason, never the value that was rejected
 * @param attempt one-based attempt counter
 * @param failedAt when the check failed
 */
public record IdentityValidationFailed(
        String appointmentId,
        String patientRef,
        String method,
        String reasonCode,
        int attempt,
        Instant failedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.IDENTITY_VALIDATION_FAILED;
    }
}
