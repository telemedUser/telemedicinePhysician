package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * Both participants joined the video room. End of the time-to-enter-video KPI.
 *
 * @param appointmentId appointment the session serves
 * @param sessionId provider-side session identifier
 * @param provider active provider
 * @param startedAt when the consultation actually began
 */
public record VideoSessionStarted(
        String appointmentId, String sessionId, String provider, Instant startedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.VIDEO_SESSION_STARTED;
    }
}
