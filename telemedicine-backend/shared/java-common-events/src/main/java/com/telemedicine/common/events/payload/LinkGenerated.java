package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A single-use access link was issued for an appointment.
 *
 * <p>The link itself is a credential and is deliberately absent: this event says one was created,
 * not what it is.
 *
 * @param appointmentId appointment the link admits to
 * @param linkId identifier of the link, safe to log
 * @param expiresAt when the link stops working
 * @param channel how it was delivered, for example {@code SMS} or {@code EMAIL}
 */
public record LinkGenerated(String appointmentId, String linkId, Instant expiresAt, String channel)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.LINK_GENERATED;
    }
}
