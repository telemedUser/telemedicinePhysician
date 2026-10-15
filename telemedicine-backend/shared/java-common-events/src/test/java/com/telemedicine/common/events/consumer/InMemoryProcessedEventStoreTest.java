package com.telemedicine.common.events.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.time.FixedTimeProvider;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryProcessedEventStoreTest {

    private final FixedTimeProvider clock = FixedTimeProvider.atEpoch();
    private final InMemoryProcessedEventStore store = new InMemoryProcessedEventStore(clock);

    @Test
    void defaultConstructorUsesSystemClock() {
        InMemoryProcessedEventStore store = new InMemoryProcessedEventStore();
        assertThat(store.markProcessed("svc", "E9", "LinkGenerated", Duration.ofHours(1))).isTrue();
    }

    @Test
    void recordsAndFindsProcessedEvents() {
        assertThat(store.markProcessed("svc", "E1", "LinkGenerated", Duration.ofHours(1))).isTrue();
        assertThat(store.find("svc", "E1"))
                .isPresent()
                .get()
                .satisfies(record -> assertThat(record.eventType()).isEqualTo("LinkGenerated"));

        assertThat(store.markProcessed("svc", "E1", "LinkGenerated", Duration.ofHours(1)))
                .isFalse();
    }

    @Test
    void purgesExpiredEntries() {
        store.markProcessed("svc", "E1", "LinkGenerated", Duration.ofHours(1));
        clock.advance(Duration.ofDays(8));

        assertThat(store.purgeExpired(Duration.ofDays(7))).isEqualTo(1);
        assertThat(store.find("svc", "E1")).isEmpty();
    }
}
