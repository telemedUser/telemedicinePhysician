package com.telemedicine.common.testing.events;

import com.telemedicine.common.events.EventEnvelope;
import java.time.Duration;
import java.util.Objects;
import org.awaitility.Awaitility;

/** Fluent assertions over events captured by {@link RecordingEventPublisher}. */
public final class EventAssertions {

    private EventAssertions() {}

    public static PublishedEventAssert assertPublished(
            RecordingEventPublisher publisher, String eventType) {
        return new PublishedEventAssert(publisher, eventType);
    }

    public static final class PublishedEventAssert {

        private final RecordingEventPublisher publisher;
        private final String eventType;
        private String subject;
        private Duration within = Duration.ofSeconds(5);

        PublishedEventAssert(RecordingEventPublisher publisher, String eventType) {
            this.publisher = Objects.requireNonNull(publisher, "publisher");
            this.eventType = Objects.requireNonNull(eventType, "eventType");
        }

        public PublishedEventAssert withSubject(String expectedSubject) {
            this.subject = expectedSubject;
            return this;
        }

        public PublishedEventAssert within(Duration timeout) {
            this.within = timeout;
            return this;
        }

        public EventEnvelope<?> get() {
            Awaitility.await()
                    .atMost(within)
                    .untilAsserted(
                            () -> {
                                var matches = publisher.ofType(eventType);
                                if (subject != null) {
                                    matches =
                                            matches.stream()
                                                    .filter(
                                                            envelope ->
                                                                    subject.equals(
                                                                            envelope.subject()))
                                                    .toList();
                                }
                                org.assertj.core.api.Assertions.assertThat(matches)
                                        .withFailMessage(
                                                "Expected event %s%s but found %s",
                                                eventType,
                                                subject == null ? "" : " for subject " + subject,
                                                publisher.all())
                                        .isNotEmpty();
                            });
            var matches = publisher.ofType(eventType);
            if (subject != null) {
                matches =
                        matches.stream()
                                .filter(envelope -> subject.equals(envelope.subject()))
                                .toList();
            }
            return matches.getLast();
        }
    }
}
