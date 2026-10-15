package com.telemedicine.common.integration.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExternalSystemCircuitBreakerTest {

    @Test
    void opensAfterFailureThreshold() {
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(4)
                        .minimumNumberOfCalls(4)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofMillis(100))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build();
        CircuitBreaker breaker = CircuitBreakerRegistry.of(config).circuitBreaker("fonasa");
        AtomicInteger attempts = new AtomicInteger();

        for (int index = 0; index < 4; index++) {
            try {
                breaker.executeSupplier(
                        () -> {
                            attempts.incrementAndGet();
                            throw new RuntimeException("down");
                        });
            } catch (RuntimeException ignored) {
                // expected
            }
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> breaker.executeSupplier(() -> "ok"))
                .isInstanceOf(
                        io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);
    }
}
