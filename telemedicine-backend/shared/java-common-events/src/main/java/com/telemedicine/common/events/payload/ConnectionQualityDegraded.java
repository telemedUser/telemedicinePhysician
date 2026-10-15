package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A participant's connection quality dropped below the acceptable threshold.
 *
 * @param appointmentId appointment affected
 * @param sessionId video session affected
 * @param participantRole {@code PATIENT} or {@code PHYSICIAN}
 * @param qualityScore provider-reported score, lower is worse
 * @param detectedAt when the degradation was observed
 */
public record ConnectionQualityDegraded(
        String appointmentId,
        String sessionId,
        String participantRole,
        int qualityScore,
        Instant detectedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CONNECTION_QUALITY_DEGRADED;
    }
}
