package com.telemedicine.common.testing.fixtures;

import com.telemedicine.common.core.time.FixedTimeProvider;

/**
 * Convenience entry point for the most common deterministic test fixtures.
 *
 * <p>Delegates to {@link FixedTimeProvider} in {@code java-common-core} and configures Awaitility
 * defaults for the whole test suite.
 */
public final class TestFixtures {

    private TestFixtures() {}

    /** Stable clock at {@code 2025-01-01T00:00:00Z}. */
    public static FixedTimeProvider fixedTime() {
        return FixedTimeProvider.atEpoch();
    }

    /** Applies Telemedicine-wide Awaitility defaults once per test class. */
    public static void configureAwaitilityDefaults() {
        AwaitilityDefaults.apply();
    }
}
