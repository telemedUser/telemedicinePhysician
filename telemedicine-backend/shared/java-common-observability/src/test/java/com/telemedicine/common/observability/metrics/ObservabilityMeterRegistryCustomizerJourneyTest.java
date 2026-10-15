package com.telemedicine.common.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ObservabilityMeterRegistryCustomizerJourneyTest {

    @Test
    void appliesSloBucketsToJourneyMetrics() {
        MockEnvironment environment = new MockEnvironment();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ObservabilityMeterRegistryCustomizer(
                        environment, new TelemedicineObservabilityProperties())
                .customize(registry);

        registry.timer(JourneyMetrics.METRIC_WAITING_TIME, "appointmentId", "a1")
                .record(java.time.Duration.ofMillis(120));

        assertThat(registry.find(JourneyMetrics.METRIC_WAITING_TIME).timer()).isNotNull();
    }
}
