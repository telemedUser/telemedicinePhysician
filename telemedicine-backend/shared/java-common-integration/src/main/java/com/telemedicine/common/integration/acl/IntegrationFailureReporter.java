package com.telemedicine.common.integration.acl;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.events.payload.IntegrationFailed;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Publishes {@link IntegrationFailed} when an adapter exhausts its resilience policy. */
public class IntegrationFailureReporter {

    private static final Logger log = LoggerFactory.getLogger(IntegrationFailureReporter.class);

    private final Optional<IntegrationEventPublisher> eventPublisher;

    public IntegrationFailureReporter(Optional<IntegrationEventPublisher> eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    public void report(String system, String operation, String subjectRef, ErrorCode errorCode) {
        IntegrationFailed payload =
                new IntegrationFailed(
                        system,
                        operation,
                        subjectRef,
                        errorCode.code(),
                        errorCode.retryable(),
                        Instant.now());
        eventPublisher.ifPresentOrElse(
                publisher -> publisher.publish(subjectRef, payload),
                () ->
                        log.warn(
                                "Integration failure for system={} operation={} subject={} code={} "
                                        + "(no EventPublisher configured)",
                                system,
                                operation,
                                subjectRef,
                                errorCode.code()));
    }

    /** SPI so the module does not hard-require Kafka at compile time for unit tests. */
    @FunctionalInterface
    public interface IntegrationEventPublisher {

        CompletableFuture<Void> publish(String subject, IntegrationFailed payload);
    }
}
