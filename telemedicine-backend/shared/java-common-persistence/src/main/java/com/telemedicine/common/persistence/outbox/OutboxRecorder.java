package com.telemedicine.common.persistence.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.TopicNames;
import com.telemedicine.common.events.outbox.OutboxEvent;
import com.telemedicine.common.events.payload.EventPayload;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records domain events in the transactional outbox within the caller's transaction.
 *
 * <p>The outbox poller ({@link OutboxPublisher}) publishes rows asynchronously; callers must not
 * publish to Kafka while holding a database transaction open.
 */
public class OutboxRecorder {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxRecorder(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(
            String aggregateType, String aggregateId, EventPayload payload, String correlationId) {
        record(aggregateType, aggregateId, payload, correlationId, aggregateId);
    }

    @Transactional
    public void record(
            String aggregateType,
            String aggregateId,
            EventPayload payload,
            String correlationId,
            String subject) {
        EventTypes.require(payload.eventType());
        OutboxEvent event = new OutboxEvent();
        event.setId(IdGenerator.eventId());
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(payload.eventType());
        event.setEventVersion(payload.schemaVersion());
        event.setTopic(TopicNames.topicFor(payload.eventType()));
        event.setCorrelationId(correlationId);
        event.setSubject(subject);
        event.setPayload(serialize(payload));
        event.setCreatedAt(Instant.now());
        event.setPublished(false);
        event.setAttempts(0);
        event.setFailed(false);
        repository.save(event);
    }

    private JsonNode serialize(EventPayload payload) {
        return objectMapper.valueToTree(payload);
    }
}
