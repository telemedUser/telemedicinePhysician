package com.telemedicine.common.testing.fixtures;

import java.time.Duration;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

/** Shared Awaitility defaults for asynchronous assertions across the platform. */
public final class AwaitilityDefaults {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(200);

    private AwaitilityDefaults() {}

    public static void apply() {
        Awaitility.setDefaultPollInterval(DEFAULT_POLL_INTERVAL);
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(DEFAULT_TIMEOUT);
    }

    public static ConditionFactory awaitDefaults() {
        return Awaitility.await().pollInterval(DEFAULT_POLL_INTERVAL).timeout(DEFAULT_TIMEOUT);
    }
}
