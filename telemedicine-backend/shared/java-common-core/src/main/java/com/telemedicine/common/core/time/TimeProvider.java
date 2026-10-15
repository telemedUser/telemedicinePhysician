package com.telemedicine.common.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Indirection over the system clock.
 *
 * <p>Outbox backoff, secure-link TTL, session timeouts, two-factor lockout windows and consent
 * expiry are all time-dependent, and none of them can be tested deterministically against {@code
 * Instant.now()}. Production code injects this; tests inject {@link FixedTimeProvider}.
 */
public interface TimeProvider {

    Instant now();

    /** Zone used for date-bucketed reporting. Defaults to UTC. */
    default ZoneId zone() {
        return ZoneId.of("UTC");
    }

    default LocalDate today() {
        return LocalDate.ofInstant(now(), zone());
    }

    default long epochMilli() {
        return now().toEpochMilli();
    }

    /** Adapter for APIs that require a {@link Clock}, such as JSR-310 and Spring Security. */
    default Clock clock() {
        return Clock.fixed(now(), zone());
    }

    static TimeProvider system() {
        return SystemTimeProvider.INSTANCE;
    }
}
