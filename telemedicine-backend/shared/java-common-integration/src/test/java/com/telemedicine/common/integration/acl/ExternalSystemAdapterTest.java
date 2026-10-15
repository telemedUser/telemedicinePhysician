package com.telemedicine.common.integration.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IntegrationException;
import com.telemedicine.common.events.payload.IntegrationFailed;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ExternalSystemAdapterTest {

    @Test
    void mapsResponseAndReportsFailures() {
        List<IntegrationFailed> events = new ArrayList<>();
        IntegrationFailureReporter reporter =
                new IntegrationFailureReporter(
                        Optional.of(
                                (subject, payload) -> {
                                    events.add(payload);
                                    return CompletableFuture.completedFuture(null);
                                }));
        TestAdapter adapter = new TestAdapter(reporter);

        assertThat(adapter.execute("req", "validate", "appt-1")).isEqualTo("OK:req");

        adapter.failNext();
        assertThatThrownBy(() -> adapter.execute("req", "validate", "appt-2"))
                .isInstanceOf(IntegrationException.class);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().system()).isEqualTo("fonasa");
        assertThat(events.getFirst().errorCode()).isEqualTo(ErrorCode.INTEGRATION_ERROR.code());
    }

    private static final class TestAdapter
            extends ExternalSystemAdapter<String, String, String, String> {

        private boolean fail;

        private TestAdapter(IntegrationFailureReporter reporter) {
            super(
                    "fonasa",
                    reporter,
                    new AntiCorruptionMapper<>() {
                        @Override
                        public String toExternal(String domainRequest) {
                            return domainRequest.toUpperCase();
                        }

                        @Override
                        public String toDomain(String externalResponse) {
                            return externalResponse;
                        }
                    });
        }

        void failNext() {
            this.fail = true;
        }

        @Override
        protected String invoke(String externalRequest) {
            if (fail) {
                throw new RuntimeException("vendor down");
            }
            return "OK:" + externalRequest.toLowerCase();
        }
    }
}
