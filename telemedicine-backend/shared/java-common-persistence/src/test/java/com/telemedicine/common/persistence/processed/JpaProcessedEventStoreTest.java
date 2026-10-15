package com.telemedicine.common.persistence.processed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.time.FixedTimeProvider;
import com.telemedicine.common.events.consumer.ProcessedEventRecord;
import com.telemedicine.common.persistence.processed.ProcessedEventEntity.ProcessedEventId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class JpaProcessedEventStoreTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void markProcessedIsIdempotent() {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        when(repository.saveAndFlush(any()))
                .thenReturn(new ProcessedEventEntity())
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        JpaProcessedEventStore store =
                new JpaProcessedEventStore(repository, new FixedTimeProvider(NOW));

        assertThat(store.markProcessed("group-a", "01JEVENT", "LinkGenerated", Duration.ofHours(1)))
                .isTrue();
        assertThat(store.markProcessed("group-a", "01JEVENT", "LinkGenerated", Duration.ofHours(1)))
                .isFalse();
    }

    @Test
    void findMapsEntityToRecord() {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setId(new ProcessedEventId("group-a", "01JEVENT"));
        entity.setEventType("LinkGenerated");
        entity.setProcessedAt(NOW);
        when(repository.findById(new ProcessedEventId("group-a", "01JEVENT")))
                .thenReturn(Optional.of(entity));

        JpaProcessedEventStore store =
                new JpaProcessedEventStore(repository, new FixedTimeProvider(NOW));

        Optional<ProcessedEventRecord> found = store.find("group-a", "01JEVENT");
        assertThat(found)
                .get()
                .satisfies(
                        record -> {
                            assertThat(record.consumerGroup()).isEqualTo("group-a");
                            assertThat(record.eventId()).isEqualTo("01JEVENT");
                            assertThat(record.eventType()).isEqualTo("LinkGenerated");
                        });
    }

    @Test
    void findReturnsEmptyWhenMissing() {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        when(repository.findById(new ProcessedEventId("group-a", "missing")))
                .thenReturn(Optional.empty());
        JpaProcessedEventStore store =
                new JpaProcessedEventStore(repository, new FixedTimeProvider(NOW));

        assertThat(store.find("group-a", "missing")).isEmpty();
    }

    @Test
    void purgeExpiredDelegatesToRepository() {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        when(repository.deleteExpired(NOW.minus(Duration.ofDays(7)))).thenReturn(3);
        JpaProcessedEventStore store =
                new JpaProcessedEventStore(repository, new FixedTimeProvider(NOW));

        assertThat(store.purgeExpired(Duration.ofDays(7))).isEqualTo(3);
        verify(repository).deleteExpired(NOW.minus(Duration.ofDays(7)));
    }
}
