package com.telemedicine.common.persistence.processed;

import com.telemedicine.common.core.time.TimeProvider;
import com.telemedicine.common.events.consumer.ProcessedEventRecord;
import com.telemedicine.common.events.consumer.ProcessedEventStore;
import com.telemedicine.common.persistence.processed.ProcessedEventEntity.ProcessedEventId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** JPA-backed {@link ProcessedEventStore} for idempotent Kafka consumers. */
public class JpaProcessedEventStore implements ProcessedEventStore {

    private final ProcessedEventRepository repository;
    private final TimeProvider clock;

    public JpaProcessedEventStore(ProcessedEventRepository repository, TimeProvider clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedEventRecord> find(String consumerGroup, String eventId) {
        return repository
                .findById(new ProcessedEventId(consumerGroup, eventId))
                .map(
                        entity ->
                                new ProcessedEventRecord(
                                        entity.getId().getConsumerGroup(),
                                        entity.getId().getEventId(),
                                        entity.getEventType(),
                                        entity.getProcessedAt()));
    }

    @Override
    @Transactional
    public boolean markProcessed(
            String consumerGroup, String eventId, String eventType, Duration ttl) {
        Instant now = clock.now();
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setId(new ProcessedEventId(consumerGroup, eventId));
        entity.setEventType(eventType);
        entity.setProcessedAt(now);
        entity.setExpiresAt(now.plus(ttl));
        try {
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    @Override
    @Transactional
    public int purgeExpired(Duration retention) {
        Instant cutoff = clock.now().minus(retention);
        return repository.deleteExpired(cutoff);
    }
}
