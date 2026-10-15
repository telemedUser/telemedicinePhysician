package com.telemedicine.common.integration.acl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IntegrationException;
import com.telemedicine.common.core.error.ProviderOutageException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ExternalSystemAdapterExtendedTest {

    @Test
    void mapsCircuitOpenAndTimeoutFailures() {
        IntegrationFailureReporter reporter =
                new IntegrationFailureReporter(
                        Optional.of((s, p) -> CompletableFuture.completedFuture(null)));

        CircuitBreaker breaker = CircuitBreaker.ofDefaults("fonasa");
        breaker.transitionToOpenState();
        FailingAdapter circuit =
                new FailingAdapter(
                        reporter,
                        CallNotPermittedException.createCallNotPermittedException(breaker));
        assertThatThrownBy(() -> circuit.execute("x", "call", "sub"))
                .isInstanceOf(IntegrationException.class)
                .extracting(ex -> ((IntegrationException) ex).getCode())
                .isEqualTo(ErrorCode.CIRCUIT_OPEN);

        FailingAdapter timeout =
                new FailingAdapter(reporter, new RuntimeException("slow", new TimeoutException()));
        assertThatThrownBy(() -> timeout.execute("x", "call", "sub"))
                .isInstanceOf(IntegrationException.class)
                .extracting(ex -> ((IntegrationException) ex).getCode())
                .isEqualTo(ErrorCode.EXTERNAL_TIMEOUT);
    }

    @Test
    void preservesProviderOutageException() {
        IntegrationFailureReporter reporter =
                new IntegrationFailureReporter(
                        Optional.of((s, p) -> CompletableFuture.completedFuture(null)));
        FailingAdapter adapter =
                new FailingAdapter(
                        reporter, new ProviderOutageException("vonage", "provider down"));

        assertThatThrownBy(() -> adapter.execute("x", "call", "sub"))
                .isInstanceOf(ProviderOutageException.class)
                .extracting(ex -> ((ProviderOutageException) ex).getCode())
                .isEqualTo(ErrorCode.PROVIDER_OUTAGE);
    }

    @Test
    void preservesExistingIntegrationException() {
        IntegrationFailureReporter reporter =
                new IntegrationFailureReporter(
                        Optional.of((s, p) -> CompletableFuture.completedFuture(null)));
        FailingAdapter adapter =
                new FailingAdapter(reporter, new IntegrationException("fonasa", "already mapped"));

        assertThatThrownBy(() -> adapter.execute("x", "call", "sub"))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("already mapped");
    }

    private static final class FailingAdapter
            extends ExternalSystemAdapter<String, String, String, String> {

        private final RuntimeException failure;

        private FailingAdapter(IntegrationFailureReporter reporter, RuntimeException failure) {
            super(
                    "fonasa",
                    reporter,
                    new AntiCorruptionMapper<>() {
                        @Override
                        public String toExternal(String domainRequest) {
                            return domainRequest;
                        }

                        @Override
                        public String toDomain(String externalResponse) {
                            return externalResponse;
                        }
                    });
            this.failure = failure;
        }

        @Override
        protected String invoke(String externalRequest) {
            throw failure;
        }
    }
}
