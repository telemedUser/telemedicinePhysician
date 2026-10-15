package com.telemedicine.common.events;

import com.telemedicine.common.events.payload.EventPayload;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes a domain event.
 *
 * <p>An interface rather than a class so that the transactional outbox, a plain Kafka producer and
 * a recording test double are interchangeable. A service that writes events inside a database
 * transaction should record them through the outbox and let its publisher run afterwards, rather
 * than calling a broker while holding a transaction open.
 */
public interface EventPublisher {

    /**
     * Sends the envelope to the topic for its type.
     *
     * @return completes when the broker has acknowledged, or fails with the send error; ignoring
     *     the result turns a delivery failure into silence
     */
    <T> CompletableFuture<Void> publish(EventEnvelope<T> envelope);

    /**
     * Publishes a payload, taking the event type from the payload itself and correlation from the
     * current MDC. Preferred at call sites: the type and the body cannot disagree.
     */
    default CompletableFuture<Void> publish(String subject, EventPayload payload) {
        return publish(EventEnvelope.forPublication(payload.eventType(), subject, payload));
    }
}
