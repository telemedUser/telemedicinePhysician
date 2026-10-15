package com.telemedicine.common.persistence.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CacheNamesTest {

    @Test
    void definesNamedCachesWithTtlPolicy() {
        assertThat(CacheNames.defaultTtls(Duration.ofMinutes(10)))
                .containsEntry(CacheNames.PROVIDER_REGISTRY, Duration.ofMinutes(5))
                .containsEntry(CacheNames.CONFIG_SNAPSHOT, Duration.ofMinutes(1))
                .containsEntry(CacheNames.CONSENT_STATUS, Duration.ofMinutes(10));
    }
}
