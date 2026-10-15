package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A notification was handed to its delivery channel.
 *
 * <p>The recipient is a reference, never an address: publishing the e-mail or mobile number a
 * message went to would put contact PHI on a Kafka topic.
 *
 * @param notificationId identifier of the notification
 * @param appointmentId appointment it relates to, null for account-level messages
 * @param channel for example {@code SMS}, {@code EMAIL} or {@code WHATSAPP}
 * @param templateId template used
 * @param recipientRef pseudonymous reference to the recipient
 * @param sentAt when it was handed off
 */
public record NotificationSent(
        String notificationId,
        String appointmentId,
        String channel,
        String templateId,
        String recipientRef,
        Instant sentAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.NOTIFICATION_SENT;
    }
}
