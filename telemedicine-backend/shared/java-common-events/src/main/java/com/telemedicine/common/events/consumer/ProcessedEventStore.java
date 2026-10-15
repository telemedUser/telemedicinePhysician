package com.telemedicine.common.events.consumer;

import java.time.Duration;
import java.util.Optional;

/**
 * Records which event ids a consumer has already handled.
 *
 * <p>The JPA implementation lives in {@code java-common-persistence}; this interface is the seam
 * that lets {@link IdempotentConsumerAspect} work in unit tests with an in-memory store.
 */
public interface ProcessedEventStore {

    /** Whether this consumer group has already processed the event. */
    Optional<ProcessedEventRecord> find(String consumerGroup, String eventId);

    /**
     * Marks an event as processed.
     *
     * @return false when another thread or replica won the race for the same id
     */
    boolean markProcessed(String consumerGroup, String eventId, String eventType, Duration ttl);

    /** Removes entries older than the retention window. */
    int purgeExpired(Duration retention);
}
