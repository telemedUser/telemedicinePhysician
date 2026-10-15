package com.telemedicine.common.observability.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ObservabilityEnvironmentPostProcessorEmptyDefaultsTest {

    @Test
    void skipsPropertySourceWhenEverythingConfigured() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("management.endpoints.web.exposure.include", "health");
        environment.setProperty("management.endpoint.health.probes.enabled", "true");
        environment.setProperty("management.health.livenessstate.enabled", "true");
        environment.setProperty("management.health.readinessstate.enabled", "true");
        environment.setProperty("management.endpoint.prometheus.enabled", "true");
        environment.setProperty("management.otlp.tracing.endpoint", "http://jaeger:4317");
        environment.setProperty("management.tracing.sampling.probability", "1.0");
        environment.setProperty("logging.config", "classpath:logback.xml");
        environment.setProperty("management.metrics.tags.application", "app");

        new ObservabilityEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(
                        environment
                                .getPropertySources()
                                .contains(ObservabilityEnvironmentPostProcessor.PROPERTY_SOURCE))
                .isFalse();
    }
}
