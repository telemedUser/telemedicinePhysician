package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The video room closed.
 *
 * <p>{@code endReason} separates a completed consultation from an abandoned one, which is what the
 * abandonment-rate KPI is computed from.
 *
 * @param appointmentId appointment the session served
 * @param sessionId provider-side session identifier
 * @param provider active provider
 * @param durationSeconds elapsed time from start to end
 * @param endReason coded reason, for example {@code COMPLETED} or {@code PATIENT_LEFT}
 * @param endedAt when the room closed
 */
public record VideoSessionEnded(
        String appointmentId,
        String sessionId,
        String provider,
        long durationSeconds,
        String endReason,
        Instant endedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.VIDEO_SESSION_ENDED;
    }
}
