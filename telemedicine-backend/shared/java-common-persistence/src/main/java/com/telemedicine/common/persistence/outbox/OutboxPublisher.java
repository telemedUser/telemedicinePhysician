package com.telemedicine.common.persistence.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.time.TimeProvider;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.TopicNames;
import com.telemedicine.common.events.outbox.OutboxEvent;
import com.telemedicine.common.persistence.TelemedicineOutboxProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Claims unpublished outbox rows with {@code FOR UPDATE SKIP LOCKED} and publishes them to Kafka.
 */
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TelemedicineOutboxProperties properties;
    private final TimeProvider clock;
    private final TransactionTemplate transactionTemplate;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public OutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            TelemedicineOutboxProperties properties,
            TimeProvider clock,
            PlatformTransactionManager transactionManager,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.publishedCounter =
                Counter.builder("outbox.published.total")
                        .description("Outbox events published to Kafka")
                        .register(meterRegistry);
        this.failedCounter =
                Counter.builder("outbox.failed.total")
                        .description("Outbox events moved to DLQ after exhausting retries")
                        .register(meterRegistry);
        Gauge.builder(
                        "outbox.pending",
                        repository,
                        OutboxEventRepository::countByPublishedFalseAndFailedFalse)
                .description("Unpublished outbox events awaiting dispatch")
                .register(meterRegistry);
    }

    public void publishPending() {
        Instant now = clock.now();
        List<OutboxEvent> claimed =
                transactionTemplate.execute(
                        status -> repository.lockPendingEvents(now, properties.getBatchSize()));
        if (claimed == null || claimed.isEmpty()) {
            return;
        }
        for (OutboxEvent event : claimed) {
            publishOne(event, now);
        }
    }

    private void publishOne(OutboxEvent event, Instant now) {
        try {
            EventEnvelope<?> envelope =
                    new EventEnvelope<>(
                            event.getEventId(),
                            event.getEventType(),
                            event.getEventVersion(),
                            event.getOccurredAt(),
                            event.getCorrelationId(),
                            event.getCausationId(),
                            event.getSubject(),
                            event.getPayload());
            String body = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(event.getTopic(), event.getEventId(), body).get();
            markPublished(event, now);
            publishedCounter.increment();
        } catch (Exception exception) {
            handleFailure(event, now, exception);
        }
    }

    private void markPublished(OutboxEvent event, Instant now) {
        transactionTemplate.executeWithoutResult(
                status -> {
                    OutboxEvent managed = repository.findById(event.getId()).orElse(event);
                    managed.setPublished(true);
                    managed.setPublishedAt(now);
                    managed.setLastError(null);
                    repository.save(managed);
                });
    }

    private void handleFailure(OutboxEvent event, Instant now, Exception exception) {
        int nextAttempt = event.getAttempts() + 1;
        String message = rootMessage(exception);
        log.warn(
                "Failed to publish outbox event {} (attempt {}/{}): {}",
                event.getEventId(),
                nextAttempt,
                properties.getMaxAttempts(),
                message);

        if (nextAttempt >= properties.getMaxAttempts()) {
            moveToDlq(event, exception);
            failedCounter.increment();
            return;
        }

        Instant nextAttemptAt = now.plus(backoffForAttempt(nextAttempt));
        transactionTemplate.executeWithoutResult(
                status -> {
                    OutboxEvent managed = repository.findById(event.getId()).orElse(event);
                    managed.setAttempts(nextAttempt);
                    managed.setLastError(message);
                    managed.setNextAttemptAt(nextAttemptAt);
                    repository.save(managed);
                });
    }

    private void moveToDlq(OutboxEvent event, Exception exception) {
        try {
            EventEnvelope<?> envelope =
                    new EventEnvelope<>(
                            event.getEventId(),
                            event.getEventType(),
                            event.getEventVersion(),
                            event.getOccurredAt(),
                            event.getCorrelationId(),
                            event.getCausationId(),
                            event.getSubject(),
                            event.getPayload());
            String body = objectMapper.writeValueAsString(envelope);
            kafkaTemplate
                    .send(TopicNames.dlqFor(event.getEventType()), event.getEventId(), body)
                    .get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing outbox event {} to DLQ", event.getEventId());
        } catch (Exception dlqException) {
            log.error(
                    "Failed to publish outbox event {} to DLQ after {} attempts",
                    event.getEventId(),
                    properties.getMaxAttempts(),
                    dlqException);
        }

        transactionTemplate.executeWithoutResult(
                status -> {
                    OutboxEvent managed = repository.findById(event.getId()).orElse(event);
                    managed.setAttempts(properties.getMaxAttempts());
                    managed.setFailed(true);
                    managed.setLastError(rootMessage(exception));
                    repository.save(managed);
                });
    }

    private Duration backoffForAttempt(int attempt) {
        long initialMillis = properties.getInitialBackoff().toMillis();
        long maxMillis = properties.getMaxBackoff().toMillis();
        double multiplier = properties.getBackoffMultiplier();
        long backoff = (long) (initialMillis * Math.pow(multiplier, Math.max(0, attempt - 1)));
        return Duration.ofMillis(Math.min(backoff, maxMillis));
    }

    private static String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }
}
