package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A chat message was exchanged during a consultation.
 *
 * <p>The text is not here and must never be added. In-consultation chat is clinical communication;
 * this event exists so activity can be counted and audited, not so the conversation can be read off
 * a Kafka topic.
 *
 * @param appointmentId appointment the chat belongs to
 * @param sessionId video session the chat belongs to
 * @param messageId identifier of the message in the provider or local store
 * @param senderRole {@code PATIENT} or {@code PHYSICIAN}
 * @param sentAt when the message was sent
 */
public record ChatMessageSent(
        String appointmentId, String sessionId, String messageId, String senderRole, Instant sentAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CHAT_MESSAGE_SENT;
    }
}
