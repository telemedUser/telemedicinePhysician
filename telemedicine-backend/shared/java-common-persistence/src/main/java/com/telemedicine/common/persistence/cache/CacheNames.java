package com.telemedicine.common.persistence.cache;

import java.time.Duration;
import java.util.Map;

/** Named cache regions and their TTL policies. */
public final class CacheNames {

    public static final String PROVIDER_REGISTRY = "providerRegistry";

    public static final String CONFIG_SNAPSHOT = "configSnapshot";

    public static final String CONSENT_STATUS = "consentStatus";

    private CacheNames() {}

    public static Map<String, Duration> defaultTtls(Duration fallback) {
        return Map.of(
                PROVIDER_REGISTRY, Duration.ofMinutes(5),
                CONFIG_SNAPSHOT, Duration.ofMinutes(1),
                CONSENT_STATUS, fallback);
    }
}
