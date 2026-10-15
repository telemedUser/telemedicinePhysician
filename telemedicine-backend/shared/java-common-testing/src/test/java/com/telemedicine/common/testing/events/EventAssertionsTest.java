package com.telemedicine.common.testing.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.payload.StatusChanged;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventAssertionsTest {

    @Test
    void assertPublishedTimesOutWhenMissing() {
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        assertThatThrownBy(
                        () ->
                                EventAssertions.assertPublished(
                                                publisher, EventTypes.STATUS_CHANGED)
                                        .within(Duration.ofMillis(200))
                                        .get())
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void recordingPublisherTracksMultipleEvents() {
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        StatusChanged payload =
                new StatusChanged(
                        "apt-1",
                        "WAITING",
                        "READY",
                        "system",
                        Instant.parse("2026-01-01T00:00:00Z"));
        publisher.publish("apt-1", payload).join();
        publisher.publish("apt-2", payload).join();

        assertThat(publisher.all()).hasSize(2);
        assertThat(publisher.ofType(EventTypes.STATUS_CHANGED)).hasSize(2);
        publisher.clear();
        assertThat(publisher.all()).isEmpty();
    }
}
