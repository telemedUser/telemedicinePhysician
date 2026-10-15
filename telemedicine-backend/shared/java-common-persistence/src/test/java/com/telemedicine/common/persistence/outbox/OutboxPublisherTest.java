package com.telemedicine.common.persistence.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.time.FixedTimeProvider;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.outbox.OutboxEvent;
import com.telemedicine.common.persistence.TelemedicineOutboxProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;

class OutboxPublisherTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    private OutboxEventRepository repository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private OutboxPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = EventObjectMappers.create();
        TelemedicineOutboxProperties properties = new TelemedicineOutboxProperties();
        properties.setMaxAttempts(3);
        properties.setInitialBackoff(Duration.ofSeconds(1));
        properties.setBatchSize(10);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any()))
                .thenReturn(mock(org.springframework.transaction.TransactionStatus.class));

        publisher =
                new OutboxPublisher(
                        repository,
                        kafkaTemplate,
                        objectMapper,
                        properties,
                        new FixedTimeProvider(NOW),
                        transactionManager,
                        new SimpleMeterRegistry());
    }

    @Test
    void publishesClaimedEventsWithoutDoubleEscapingPayload() throws Exception {
        OutboxEvent event = pendingEvent();
        when(repository.lockPendingEvents(NOW, 10)).thenReturn(List.of(event));
        when(repository.findById("01JOUTBOX")).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(eq("LinkGenerated"), eq("01JOUTBOX"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishPending();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("LinkGenerated"), eq("01JOUTBOX"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("\"appointmentId\":\"APT-1\"");
        assertThat(bodyCaptor.getValue()).doesNotContain("\\\"appointmentId\\\"");
        assertThat(event.isPublished()).isTrue();
    }

    @Test
    void schedulesRetryBeforeMovingToDlq() throws Exception {
        OutboxEvent event = pendingEvent();
        when(repository.lockPendingEvents(NOW, 10)).thenReturn(List.of(event));
        when(repository.findById("01JOUTBOX")).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(eq("LinkGenerated"), eq("01JOUTBOX"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        publisher.publishPending();

        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(NOW);
        assertThat(event.isFailed()).isFalse();
        verify(kafkaTemplate, never()).send(eq("LinkGenerated.DLQ"), any(), any());
    }

    @Test
    void movesToDlqAfterMaxAttempts() throws Exception {
        OutboxEvent event = pendingEvent();
        event.setAttempts(2);
        when(repository.lockPendingEvents(NOW, 10)).thenReturn(List.of(event));
        when(repository.findById("01JOUTBOX")).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(eq("LinkGenerated"), eq("01JOUTBOX"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));
        when(kafkaTemplate.send(eq("LinkGenerated.DLQ"), eq("01JOUTBOX"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishPending();

        assertThat(event.isFailed()).isTrue();
        assertThat(event.getAttempts()).isEqualTo(3);
        verify(kafkaTemplate).send(eq("LinkGenerated.DLQ"), eq("01JOUTBOX"), any());
    }

    @Test
    void skipsWorkWhenClaimReturnsNull() {
        when(repository.lockPendingEvents(NOW, 10)).thenReturn(null);

        publisher.publishPending();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void ignoresEmptyClaimBatch() {
        when(repository.lockPendingEvents(NOW, 10)).thenReturn(List.of());

        publisher.publishPending();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    private static OutboxEvent pendingEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId("01JOUTBOX");
        event.setEventType(EventTypes.LINK_GENERATED);
        event.setEventVersion("1.0");
        event.setTopic(EventTypes.LINK_GENERATED);
        event.setSubject("APT-1");
        event.setCorrelationId("01JCORR");
        event.setCreatedAt(NOW);
        event.setPublished(false);
        event.setAttempts(0);
        event.setFailed(false);
        event.setPayload(
                EventObjectMappers.create()
                        .createObjectNode()
                        .put("appointmentId", "APT-1")
                        .put("linkId", "LNK-1"));
        return event;
    }
}
