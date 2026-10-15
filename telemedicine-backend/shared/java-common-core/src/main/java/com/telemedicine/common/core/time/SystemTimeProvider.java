package com.telemedicine.common.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Wall-clock implementation backed by {@link Clock#systemUTC()}. */
public final class SystemTimeProvider implements TimeProvider {

    static final SystemTimeProvider INSTANCE = new SystemTimeProvider();

    private final Clock clock;

    public SystemTimeProvider() {
        this(Clock.systemUTC());
    }

    public SystemTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }

    @Override
    public ZoneId zone() {
        return clock.getZone();
    }

    @Override
    public Clock clock() {
        return clock;
    }
}
