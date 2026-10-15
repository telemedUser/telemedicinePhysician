package com.telemedicine.common.events.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.payload.LinkGenerated;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaEventPublisherTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void failsWhenSerialisationBreaks() {
        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        ObjectMapper broken = mock(ObjectMapper.class);
        KafkaEventPublisher publisher = new KafkaEventPublisher(template, broken, "svc");
        try {
            when(broken.writeValueAsBytes(any()))
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("bad") {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new AssertionError(exception);
        }

        EventEnvelope<String> envelope =
                EventEnvelope.<String>builder()
                        .type(EventTypes.CONFIGURATION_CHANGED)
                        .subject("GLOBAL")
                        .payload("x")
                        .build();

        assertThat(publisher.publish(envelope))
                .failsWithin(java.time.Duration.ofSeconds(1))
                .withThrowableThat()
                .havingCause()
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }

    @Test
    void logsAndFailsWhenKafkaSendFails() {
        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        when(template.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        KafkaEventPublisher publisher =
                new KafkaEventPublisher(template, EventObjectMappers.create(), null);
        EventEnvelope<String> envelope =
                EventEnvelope.<String>builder()
                        .type(EventTypes.CONFIGURATION_CHANGED)
                        .subject("GLOBAL")
                        .payload("x")
                        .build();

        assertThat(publisher.publish(envelope))
                .failsWithin(java.time.Duration.ofSeconds(1))
                .withThrowableThat()
                .havingCause()
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsToTheTopicNamedAfterTheEventType() {
        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        ArgumentCaptor<ProducerRecord<String, byte[]>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        RecordMetadata metadata =
                new RecordMetadata(new TopicPartition(EventTypes.LINK_GENERATED, 0), 0, 0, 0, 0, 0);
        SendResult<String, byte[]> result = new SendResult<>(null, metadata);
        when(template.send(captor.capture())).thenReturn(CompletableFuture.completedFuture(result));

        KafkaEventPublisher publisher =
                new KafkaEventPublisher(
                        template, EventObjectMappers.create(), "waiting-room-service");
        LinkGenerated payload =
                new LinkGenerated("APT-1", "LNK-1", Instant.parse("2026-06-01T12:00:00Z"), "SMS");
        EventEnvelope<LinkGenerated> envelope =
                EventEnvelope.<LinkGenerated>builder()
                        .eventId("01JTEST")
                        .type(EventTypes.LINK_GENERATED)
                        .subject("APT-1")
                        .payload(payload)
                        .build();

        publisher.publish(envelope).join();

        ProducerRecord<String, byte[]> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(EventTypes.LINK_GENERATED);
        assertThat(record.key()).isEqualTo("APT-1");
        assertThat(new String(record.headers().lastHeader("ce_id").value())).isEqualTo("01JTEST");
        assertThat(new String(record.value())).contains("01JTEST");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prefersEnvelopeCorrelationOverMdc() {
        MDC.put(MdcKeys.CORRELATION_ID, "from-mdc");
        MDC.put(MdcKeys.TENANT_ID, "tenant-1");

        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        ArgumentCaptor<ProducerRecord<String, byte[]>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        RecordMetadata metadata =
                new RecordMetadata(new TopicPartition(EventTypes.LINK_GENERATED, 0), 0, 0, 0, 0, 0);
        when(template.send(captor.capture()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, metadata)));

        KafkaEventPublisher publisher =
                new KafkaEventPublisher(template, EventObjectMappers.create(), "svc");
        EventEnvelope<String> envelope =
                EventEnvelope.<String>builder()
                        .type(EventTypes.LINK_GENERATED)
                        .subject("APT-1")
                        .correlationId("from-envelope")
                        .payload("x")
                        .build();

        publisher.publish(envelope).join();

        ProducerRecord<String, byte[]> record = captor.getValue();
        assertThat(new String(record.headers().lastHeader("X-Correlation-Id").value()))
                .isEqualTo("from-envelope");
        assertThat(new String(record.headers().lastHeader("X-Tenant-Id").value()))
                .isEqualTo("tenant-1");
    }
}
