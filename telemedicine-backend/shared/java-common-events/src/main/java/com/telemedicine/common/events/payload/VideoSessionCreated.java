package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A video room was provisioned with the active provider.
 *
 * <p>The provider is published because the platform switches between Vonage and Azure Communication
 * Services, and a consultation must be traceable to the provider that served it.
 *
 * @param appointmentId appointment the session serves
 * @param sessionId provider-side session identifier
 * @param provider active provider, for example {@code vonage}
 * @param createdAt when the room was created
 */
public record VideoSessionCreated(
        String appointmentId, String sessionId, String provider, Instant createdAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.VIDEO_SESSION_CREATED;
    }
}
