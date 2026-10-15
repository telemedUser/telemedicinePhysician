package com.telemedicine.common.testing.events;

import static com.telemedicine.common.testing.events.EventAssertions.assertPublished;
import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.payload.LinkGenerated;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecordingEventPublisherTest {

    @Test
    void capturesPublishedEvents() {
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        LinkGenerated payload =
                new LinkGenerated("apt-1", "link-1", Instant.parse("2026-01-02T00:00:00Z"), "SMS");

        publisher.publish("apt-1", payload).join();

        assertThat(publisher.ofType(EventTypes.LINK_GENERATED)).hasSize(1);
        assertPublished(publisher, EventTypes.LINK_GENERATED)
                .withSubject("apt-1")
                .within(Duration.ofSeconds(1));
    }
}
