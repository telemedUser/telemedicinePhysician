package com.telemedicine.common.observability.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ObservabilityEnvironmentPostProcessorTest {

    @Test
    void appliesDefaultsWhenNotConfigured() {
        MockEnvironment environment = new MockEnvironment();
        new ObservabilityEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.endpoint.health.probes.enabled"))
                .isEqualTo("true");
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .contains("prometheus");
        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://jaeger:4317");
    }

    @Test
    void doesNotOverrideExplicitValues() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("management.otlp.tracing.endpoint", "http://custom:4317");
        new ObservabilityEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://custom:4317");
    }
}
