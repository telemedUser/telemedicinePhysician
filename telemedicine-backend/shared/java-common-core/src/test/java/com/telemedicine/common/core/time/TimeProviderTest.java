package com.telemedicine.common.core.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TimeProviderTest {

    @Test
    void systemProviderTracksWallClock() {
        TimeProvider provider = TimeProvider.system();

        Instant before = Instant.now().minusSeconds(1);
        Instant now = provider.now();

        assertThat(now).isAfter(before);
        assertThat(provider.zone()).isEqualTo(ZoneId.of("Z"));
        assertThat(provider.epochMilli()).isPositive();
        assertThat(provider.clock()).isNotNull();
    }

    @Test
    void systemProviderCanWrapAnInjectedClock() {
        Clock fixed = Clock.fixed(Instant.parse("2025-06-01T10:15:30Z"), ZoneId.of("UTC"));
        SystemTimeProvider provider = new SystemTimeProvider(fixed);

        assertThat(provider.now()).isEqualTo(Instant.parse("2025-06-01T10:15:30Z"));
        assertThat(provider.clock()).isSameAs(fixed);
        assertThat(provider.today()).isEqualTo(LocalDate.of(2025, 6, 1));
    }

    @Test
    void fixedProviderOnlyMovesWhenToldTo() {
        FixedTimeProvider provider = FixedTimeProvider.atEpoch();
        Instant start = provider.now();

        assertThat(provider.now()).isEqualTo(start);

        provider.advance(Duration.ofMinutes(15));

        assertThat(provider.now()).isEqualTo(start.plus(Duration.ofMinutes(15)));
        assertThat(provider.epochMilli())
                .isEqualTo(start.plus(Duration.ofMinutes(15)).toEpochMilli());
    }

    @Test
    void fixedProviderSupportsAbsoluteResets() {
        FixedTimeProvider provider = new FixedTimeProvider(Instant.parse("2025-03-01T00:00:00Z"));

        provider.set(Instant.parse("2025-12-25T09:00:00Z"));

        assertThat(provider.now()).isEqualTo(Instant.parse("2025-12-25T09:00:00Z"));
        assertThat(provider.today()).isEqualTo(LocalDate.of(2025, 12, 25));
        assertThat(provider.clock().instant()).isEqualTo(provider.now());
    }

    @Test
    void fixedProviderHonoursANonUtcZone() {
        FixedTimeProvider provider =
                new FixedTimeProvider(
                        Instant.parse("2025-03-01T02:00:00Z"), ZoneId.of("America/Santiago"));

        assertThat(provider.zone()).isEqualTo(ZoneId.of("America/Santiago"));
        assertThat(provider.today()).isEqualTo(LocalDate.of(2025, 2, 28));
    }
}
