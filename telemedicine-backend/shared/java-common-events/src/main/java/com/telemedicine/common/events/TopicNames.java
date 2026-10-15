package com.telemedicine.common.events;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the Kafka topic, retry topic and dead-letter topic for an event type.
 *
 * <p>The convention is the one the Phase 0 topic-creation script already uses: the topic is named
 * after the event, with {@code .DLQ} and {@code .RETRY} companions. Deriving the names instead of
 * writing them out is what lets the contract test prove the broker has a topic for every event.
 */
public final class TopicNames {

    public static final String DLQ_SUFFIX = ".DLQ";
    public static final String RETRY_SUFFIX = ".RETRY";

    private TopicNames() {}

    /** Main topic for an event type. */
    public static String topicFor(String eventType) {
        return EventTypes.require(eventType);
    }

    /** Dead-letter topic, where a record lands once retries are exhausted. */
    public static String dlqFor(String eventType) {
        return topicFor(eventType) + DLQ_SUFFIX;
    }

    /** Retry topic, for consumers that defer rather than block the partition. */
    public static String retryFor(String eventType) {
        return topicFor(eventType) + RETRY_SUFFIX;
    }

    /** Dead-letter topic for an arbitrary topic name, including one that is already a retry. */
    public static String dlqForTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic is required");
        }
        if (topic.endsWith(DLQ_SUFFIX)) {
            return topic;
        }
        String base =
                topic.endsWith(RETRY_SUFFIX)
                        ? topic.substring(0, topic.length() - RETRY_SUFFIX.length())
                        : topic;
        return base + DLQ_SUFFIX;
    }

    /**
     * Every topic the platform expects to exist: each event plus its retry and dead-letter pair.
     */
    public static List<String> allTopics() {
        List<String> topics = new ArrayList<>(EventTypes.ALL.size() * 3);
        topics.addAll(EventTypes.ALL);
        EventTypes.ALL.forEach(type -> topics.add(type + DLQ_SUFFIX));
        EventTypes.ALL.forEach(type -> topics.add(type + RETRY_SUFFIX));
        return List.copyOf(topics);
    }
}
