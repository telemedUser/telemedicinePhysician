package com.telemedicine.common.web.idempotency;

import com.telemedicine.common.core.time.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-JVM {@link IdempotencyStore}, used as the fallback when no shared store is configured.
 *
 * <p>Correct for a single replica and for tests. It provides no guarantee across instances, so the
 * auto-configuration logs a warning when it is selected: relying on it behind a load balancer
 * silently reintroduces the duplicate-submission bug the filter is meant to close.
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final TimeProvider timeProvider;

    public InMemoryIdempotencyStore() {
        this(TimeProvider.system());
    }

    public InMemoryIdempotencyStore(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public boolean tryReserve(String key, String fingerprint, Duration ttl) {
        Instant now = timeProvider.now();
        purgeExpired(now);
        Entry created = new Entry(IdempotencyRecord.inFlight(key, fingerprint, now), now.plus(ttl));
        return entries.putIfAbsent(key, created) == null;
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt.isBefore(timeProvider.now())) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.record);
    }

    @Override
    public void complete(
            String key, int status, String responseBody, String contentType, Duration ttl) {
        entries.computeIfPresent(
                key,
                (ignored, entry) ->
                        new Entry(
                                entry.record.complete(status, responseBody, contentType),
                                timeProvider.now().plus(ttl)));
    }

    @Override
    public void release(String key) {
        entries.remove(key);
    }

    /** Visible for tests and for the auto-configuration's shutdown hook. */
    public int size() {
        purgeExpired(timeProvider.now());
        return entries.size();
    }

    private void purgeExpired(Instant now) {
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }

    private record Entry(IdempotencyRecord record, Instant expiresAt) {}
}
