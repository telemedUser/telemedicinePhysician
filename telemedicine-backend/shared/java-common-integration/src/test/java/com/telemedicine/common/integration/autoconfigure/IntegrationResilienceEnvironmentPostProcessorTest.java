package com.telemedicine.common.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.ExternalSystems;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class IntegrationResilienceEnvironmentPostProcessorTest {

    @Test
    void appliesNamedInstancesForAllExternalSystems() {
        MockEnvironment environment = new MockEnvironment();
        new IntegrationResilienceEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        for (String system : ExternalSystems.ALL) {
            assertThat(
                            environment.getProperty(
                                    "resilience4j.circuitbreaker.instances."
                                            + system
                                            + ".baseConfig"))
                    .isEqualTo("telemedicine-external");
            assertThat(
                            environment.getProperty(
                                    "resilience4j.retry.instances." + system + ".baseConfig"))
                    .isEqualTo("telemedicine-external");
        }
        assertThat(
                        environment.getProperty(
                                "resilience4j.circuitbreaker.configs.telemedicine-external.waitDurationInOpenState"))
                .isEqualTo("30s");
    }

    @Test
    void doesNotOverrideExplicitValues() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("resilience4j.circuitbreaker.instances.his.baseConfig", "custom");
        new IntegrationResilienceEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("resilience4j.circuitbreaker.instances.his.baseConfig"))
                .isEqualTo("custom");
    }
}
