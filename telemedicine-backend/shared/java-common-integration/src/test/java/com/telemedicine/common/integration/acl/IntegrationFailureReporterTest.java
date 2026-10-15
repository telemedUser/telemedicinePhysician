package com.telemedicine.common.integration.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.events.payload.IntegrationFailed;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IntegrationFailureReporterTest {

    @Test
    void logsWhenPublisherMissing() {
        IntegrationFailureReporter reporter = new IntegrationFailureReporter(Optional.empty());

        reporter.report("his", "lookup", "appt-1", ErrorCode.INTEGRATION_ERROR);

        assertThat(reporter).isNotNull();
    }

    @Test
    void publishesWhenPublisherPresent() {
        IntegrationFailed[] captured = new IntegrationFailed[1];
        IntegrationFailureReporter reporter =
                new IntegrationFailureReporter(
                        Optional.of(
                                (subject, payload) -> {
                                    captured[0] = payload;
                                    return java.util.concurrent.CompletableFuture.completedFuture(
                                            null);
                                }));

        reporter.report("sap", "sync", "appt-9", ErrorCode.EXTERNAL_TIMEOUT);

        assertThat(captured[0].system()).isEqualTo("sap");
        assertThat(captured[0].operation()).isEqualTo("sync");
        assertThat(captured[0].errorCode()).isEqualTo(ErrorCode.EXTERNAL_TIMEOUT.code());
    }
}
