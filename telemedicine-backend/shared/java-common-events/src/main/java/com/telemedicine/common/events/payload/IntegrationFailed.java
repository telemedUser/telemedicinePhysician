package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A call to an external system failed after the resilience policy was exhausted.
 *
 * <p>Emitted by the integration layer so that operations can see provider trouble as events rather
 * than only as log lines, and so a compensating flow can react.
 *
 * @param system external system key, for example {@code fonasa} or {@code vonage}
 * @param operation logical operation that failed
 * @param subjectRef pseudonymous reference to what the call was about, usually an appointment
 * @param errorCode catalogue error code the failure mapped to
 * @param retryable whether retrying the same call could plausibly succeed
 * @param failedAt when the failure was recorded
 */
public record IntegrationFailed(
        String system,
        String operation,
        String subjectRef,
        String errorCode,
        boolean retryable,
        Instant failedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.INTEGRATION_FAILED;
    }
}
