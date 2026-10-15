package com.telemedicine.common.events;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.EventPayload;
import java.time.Instant;
import org.slf4j.MDC;

/**
 * The body of every domain event.
 *
 * <p>The shape is frozen. Adding a record component changes the serialised form for every consumer
 * and breaks binary compatibility, so new metadata — W3C {@code traceparent}, tenant, schema URI —
 * travels in Kafka record headers instead. See {@link EventHeaders}, which is also where that
 * metadata belongs: it describes the message, not the fact that occurred.
 *
 * @param eventId unique per event, used as the deduplication key by idempotent consumers
 * @param type value from {@link EventTypes}
 * @param version payload schema version, for example {@code 1.0}
 * @param occurredAt when the fact happened, not when it was published
 * @param correlationId journey-wide identifier, carried from the inbound HTTP request
 * @param causationId identifier of the message or request that caused this one
 * @param subject aggregate this event is about; also the partition key, so events for one
 *     appointment stay ordered
 * @param payload PHI-free body, normally a record from {@code
 *     com.telemedicine.common.events.payload}
 */
public record EventEnvelope<T>(
        String eventId,
        String type,
        String version,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String subject,
        T payload) {

    /** Version assumed when a publisher does not state one. */
    public static final String DEFAULT_VERSION = "1.0";

    public static <T> EventEnvelope<T> of(
            String type, String subject, T payload, String correlationId) {
        return of(type, subject, payload, correlationId, null);
    }

    public static <T> EventEnvelope<T> of(
            String type, String subject, T payload, String correlationId, String causationId) {
        return EventEnvelope.<T>builder()
                .type(type)
                .subject(subject)
                .payload(payload)
                .correlationId(correlationId)
                .causationId(causationId)
                .build();
    }

    /**
     * Envelope for immediate publication, taking correlation and causation from the current MDC.
     *
     * <p>Reading the MDC here rather than at each call site is what keeps a trace intact: the one
     * publisher that forgets to pass the correlation id is the one that breaks an incident
     * investigation.
     */
    public static <T> EventEnvelope<T> forPublication(String type, String subject, T payload) {
        Builder<T> builder =
                EventEnvelope.<T>builder()
                        .type(type)
                        .subject(subject)
                        .payload(payload)
                        .correlationId(MDC.get(MdcKeys.CORRELATION_ID))
                        .causationId(MDC.get(MdcKeys.CAUSATION_ID));
        if (payload instanceof EventPayload typed) {
            builder.version(typed.schemaVersion());
        }
        return builder.build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** Copy of this envelope with a different payload, preserving identity and correlation. */
    public <R> EventEnvelope<R> withPayload(R newPayload) {
        return new EventEnvelope<>(
                eventId,
                type,
                version,
                occurredAt,
                correlationId,
                causationId,
                subject,
                newPayload);
    }

    /** Fluent builder that fills in the identifier, timestamp and version. */
    public static final class Builder<T> {

        private String eventId;
        private String type;
        private String version = DEFAULT_VERSION;
        private Instant occurredAt;
        private String correlationId;
        private String causationId;
        private String subject;
        private T payload;

        private Builder() {}

        public Builder<T> eventId(String value) {
            this.eventId = value;
            return this;
        }

        public Builder<T> type(String value) {
            this.type = value;
            return this;
        }

        public Builder<T> version(String value) {
            this.version = value;
            return this;
        }

        public Builder<T> occurredAt(Instant value) {
            this.occurredAt = value;
            return this;
        }

        public Builder<T> correlationId(String value) {
            this.correlationId = value;
            return this;
        }

        public Builder<T> causationId(String value) {
            this.causationId = value;
            return this;
        }

        public Builder<T> subject(String value) {
            this.subject = value;
            return this;
        }

        public Builder<T> payload(T value) {
            this.payload = value;
            return this;
        }

        /**
         * @throws IllegalArgumentException when the type is absent or not in the catalogue, or when
         *     the subject is missing — an event with no subject has no partition key and would lose
         *     ordering
         */
        public EventEnvelope<T> build() {
            EventTypes.require(type);
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException(
                        "Event " + type + " needs a subject: it is the Kafka partition key.");
            }
            return new EventEnvelope<>(
                    eventId == null ? IdGenerator.eventId() : eventId,
                    type,
                    version == null || version.isBlank() ? DEFAULT_VERSION : version,
                    occurredAt == null ? Instant.now() : occurredAt,
                    correlationId,
                    causationId,
                    subject,
                    payload);
        }
    }
}
