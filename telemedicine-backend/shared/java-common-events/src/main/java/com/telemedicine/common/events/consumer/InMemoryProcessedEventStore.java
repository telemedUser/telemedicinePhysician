package com.telemedicine.common.events.consumer;

import com.telemedicine.common.core.time.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Test and single-instance fallback for {@link ProcessedEventStore}. */
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final TimeProvider clock;
    private final Map<String, ProcessedEventRecord> records = new ConcurrentHashMap<>();

    public InMemoryProcessedEventStore() {
        this(TimeProvider.system());
    }

    public InMemoryProcessedEventStore(TimeProvider clock) {
        this.clock = clock;
    }

    @Override
    public Optional<ProcessedEventRecord> find(String consumerGroup, String eventId) {
        return Optional.ofNullable(records.get(key(consumerGroup, eventId)));
    }

    @Override
    public boolean markProcessed(
            String consumerGroup, String eventId, String eventType, Duration ttl) {
        String composite = key(consumerGroup, eventId);
        Instant now = clock.now();
        return records.putIfAbsent(
                        composite, new ProcessedEventRecord(consumerGroup, eventId, eventType, now))
                == null;
    }

    @Override
    public int purgeExpired(Duration retention) {
        Instant cutoff = clock.now().minus(retention);
        int removed = 0;
        for (Map.Entry<String, ProcessedEventRecord> entry : records.entrySet()) {
            if (entry.getValue().processedAt().isBefore(cutoff)
                    && records.remove(entry.getKey()) != null) {
                removed++;
            }
        }
        return removed;
    }

    private static String key(String consumerGroup, String eventId) {
        return consumerGroup + ':' + eventId;
    }
}
