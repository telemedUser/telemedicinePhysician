package com.telemedicine.common.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ObservabilityMeterRegistryCustomizerBranchTest {

    @Test
    void leavesNonHttpMetricsUntouched() {
        MockEnvironment environment = new MockEnvironment();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ObservabilityMeterRegistryCustomizer(
                        environment, new TelemedicineObservabilityProperties())
                .customize(registry);

        registry.counter("custom.counter").increment();
        assertThat(registry.find("custom.counter").counter()).isNotNull();
    }
}
