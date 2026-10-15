package com.telemedicine.common.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ObservabilityMeterRegistryCustomizerTest {

    @Test
    void appliesCommonTagsAndSloBuckets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.application.name", "obs-test");
        TelemedicineObservabilityProperties properties = new TelemedicineObservabilityProperties();

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ObservabilityMeterRegistryCustomizer(environment, properties).customize(registry);

        registry.timer("http.server.requests", "uri", "/health")
                .record(java.time.Duration.ofMillis(150));

        assertThat(registry.find("http.server.requests").timer().getId().getTag("service"))
                .isEqualTo("obs-test");
    }
}
