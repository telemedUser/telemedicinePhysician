package com.telemedicine.common.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JourneyMetricsTest {

    @Test
    void recordsJourneyTimersAndCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JourneyMetrics metrics = new JourneyMetrics(registry);

        Timer.Sample waiting = metrics.startWaitingTimeSample();
        metrics.recordWaitingTime(waiting, "appt-1");
        metrics.recordConsultationCompleted("appt-1");
        metrics.recordConsultationAbandoned("appt-1", "timeout");
        metrics.recordDocumentIssuanceTime(Duration.ofSeconds(2), "prescription");

        assertThat(registry.find(JourneyMetrics.METRIC_WAITING_TIME).timer()).isNotNull();
        assertThat(registry.find(JourneyMetrics.METRIC_CONSULTATION_COMPLETED).counter())
                .isNotNull();
        assertThat(registry.find(JourneyMetrics.METRIC_CONSULTATION_ABANDONED).counter())
                .isNotNull();
        assertThat(registry.find(JourneyMetrics.METRIC_DOCUMENT_ISSUANCE_TIME).timer()).isNotNull();
    }
}
