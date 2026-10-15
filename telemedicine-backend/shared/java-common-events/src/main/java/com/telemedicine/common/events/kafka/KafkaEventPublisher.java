package com.telemedicine.common.events.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventHeaders;
import com.telemedicine.common.events.EventPublisher;
import com.telemedicine.common.events.TopicNames;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * Idempotent Kafka producer with CloudEvents headers.
 *
 * <p>The record key is the envelope subject so all events for one appointment land on the same
 * partition and stay ordered. The {@code eventId} is carried in headers and in the body for
 * deduplication by consumers.
 */
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String source;

    public KafkaEventPublisher(
            KafkaTemplate<String, byte[]> kafkaTemplate, ObjectMapper objectMapper, String source) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.source = source == null || source.isBlank() ? "telemedicine" : source;
    }

    @Override
    public <T> CompletableFuture<Void> publish(EventEnvelope<T> envelope) {
        String topic = TopicNames.topicFor(envelope.type());
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(envelope);
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(topic, envelope.subject(), body);
        addHeader(record, EventHeaders.CE_SPEC_VERSION, EventHeaders.SPEC_VERSION_VALUE);
        addHeader(record, EventHeaders.CE_ID, envelope.eventId());
        addHeader(record, EventHeaders.CE_TYPE, envelope.type());
        addHeader(record, EventHeaders.CE_SOURCE, source);
        addHeader(record, EventHeaders.CE_SUBJECT, envelope.subject());
        addHeader(record, EventHeaders.CE_TIME, envelope.occurredAt().toString());
        addHeader(record, EventHeaders.CE_DATA_CONTENT_TYPE, EventHeaders.CONTENT_TYPE_JSON);
        addHeader(
                record,
                EventHeaders.CE_DATA_SCHEMA,
                EventHeaders.schemaUri(envelope.type(), envelope.version()));
        addHeader(record, EventHeaders.SCHEMA_VERSION, envelope.version());
        addHeader(
                record,
                EventHeaders.CORRELATION_ID,
                firstNonBlank(envelope.correlationId(), MDC.get(MdcKeys.CORRELATION_ID)));
        addHeader(
                record,
                EventHeaders.CAUSATION_ID,
                firstNonBlank(envelope.causationId(), MDC.get(MdcKeys.CAUSATION_ID)));
        addHeader(record, EventHeaders.TRACEPARENT, MDC.get(MdcKeys.TRACE_ID));
        addHeader(record, EventHeaders.TENANT_ID, MDC.get(MdcKeys.TENANT_ID));

        CompletableFuture<SendResult<String, byte[]>> send = kafkaTemplate.send(record);
        return send.whenComplete(
                        (result, error) -> {
                            if (error != null) {
                                log.error(
                                        "Failed to publish {} to {} for subject {}: {}",
                                        envelope.eventId(),
                                        topic,
                                        envelope.subject(),
                                        error.toString());
                            } else if (log.isDebugEnabled()) {
                                log.debug(
                                        "Published {} to {} partition {} offset {}",
                                        envelope.eventId(),
                                        topic,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            }
                        })
                .thenApply(ignored -> null);
    }

    private static void addHeader(
            ProducerRecord<String, byte[]> record, String name, String value) {
        if (value != null && !value.isBlank()) {
            record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
