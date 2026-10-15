package com.telemedicine.common.testing.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.time.FixedTimeProvider;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TestFixturesTest {

    @Test
    void syntheticPhiIsObviouslyFake() {
        assertThat(SyntheticPhiFixtures.EMAIL).endsWith(".invalid");
        assertThat(SyntheticPhiFixtures.RUT).isEqualTo("11.111.111-1");
    }

    @Test
    void fixedTimeStartsAtEpoch() {
        FixedTimeProvider clock = TestFixtures.fixedTime();
        assertThat(clock.now()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
        clock.advance(Duration.ofHours(1));
        assertThat(clock.now()).isEqualTo(Instant.parse("2025-01-01T01:00:00Z"));
    }

    @Test
    void awaitilityDefaultsAreStable() {
        TestFixtures.configureAwaitilityDefaults();
        assertThat(AwaitilityDefaults.DEFAULT_TIMEOUT).isEqualTo(Duration.ofSeconds(10));
    }
}
