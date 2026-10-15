package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A notification could not be delivered.
 *
 * @param notificationId identifier of the notification
 * @param appointmentId appointment it relates to, null for account-level messages
 * @param channel channel that failed
 * @param templateId template used
 * @param reasonCode coded failure reason, never the provider's raw message
 * @param failedAt when delivery failed
 */
public record NotificationFailed(
        String notificationId,
        String appointmentId,
        String channel,
        String templateId,
        String reasonCode,
        Instant failedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.NOTIFICATION_FAILED;
    }
}
