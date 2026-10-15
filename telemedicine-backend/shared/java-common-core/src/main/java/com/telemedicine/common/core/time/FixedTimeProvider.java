package com.telemedicine.common.core.time;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test double whose clock only moves when the test moves it.
 *
 * <p>Lives in main rather than test sources so every module and service can use it without
 * depending on another module's test fixtures.
 */
public final class FixedTimeProvider implements TimeProvider {

    private final AtomicReference<Instant> current;
    private final ZoneId zone;

    public FixedTimeProvider(Instant start) {
        this(start, ZoneId.of("UTC"));
    }

    public FixedTimeProvider(Instant start, ZoneId zone) {
        this.current = new AtomicReference<>(start);
        this.zone = zone;
    }

    /** Starts at a stable, obviously-synthetic instant so failures read consistently. */
    public static FixedTimeProvider atEpoch() {
        return new FixedTimeProvider(Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Override
    public Instant now() {
        return current.get();
    }

    @Override
    public ZoneId zone() {
        return zone;
    }

    public void advance(Duration amount) {
        current.updateAndGet(instant -> instant.plus(amount));
    }

    public void set(Instant instant) {
        current.set(instant);
    }
}
