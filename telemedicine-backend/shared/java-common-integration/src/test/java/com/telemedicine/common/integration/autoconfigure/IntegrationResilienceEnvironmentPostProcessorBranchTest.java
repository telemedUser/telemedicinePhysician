package com.telemedicine.common.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class IntegrationResilienceEnvironmentPostProcessorBranchTest {

    @Test
    void appliesBulkheadRetryAndRateLimiterDefaults() {
        MockEnvironment environment = new MockEnvironment();
        new IntegrationResilienceEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(
                        environment.getProperty(
                                "resilience4j.bulkhead.configs.telemedicine-external.maxConcurrentCalls"))
                .isEqualTo("25");
        assertThat(
                        environment.getProperty(
                                "resilience4j.retry.configs.telemedicine-external.enableRandomizedWait"))
                .isEqualTo("true");
        assertThat(
                        environment.getProperty(
                                "resilience4j.ratelimiter.configs.telemedicine-external.limitForPeriod"))
                .isEqualTo("50");
        assertThat(environment.getProperty("resilience4j.timelimiter.instances.payment.baseConfig"))
                .isEqualTo("telemedicine-external");
    }

    @Test
    void skipsWhenAllPropertiesPresent() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(
                "resilience4j.circuitbreaker.configs.telemedicine-external.slidingWindowSize",
                "99");
        environment.setProperty(
                "resilience4j.retry.configs.telemedicine-external.maxAttempts", "1");
        environment.setProperty(
                "resilience4j.bulkhead.configs.telemedicine-external.maxConcurrentCalls", "1");
        environment.setProperty(
                "resilience4j.timelimiter.configs.telemedicine-external.timeoutDuration", "1s");
        environment.setProperty(
                "resilience4j.ratelimiter.configs.telemedicine-external.limitForPeriod", "1");
        for (String system : com.telemedicine.common.integration.ExternalSystems.ALL) {
            environment.setProperty(
                    "resilience4j.circuitbreaker.instances." + system + ".baseConfig", "custom");
            environment.setProperty(
                    "resilience4j.retry.instances." + system + ".baseConfig", "custom");
            environment.setProperty(
                    "resilience4j.bulkhead.instances." + system + ".baseConfig", "custom");
            environment.setProperty(
                    "resilience4j.timelimiter.instances." + system + ".baseConfig", "custom");
            environment.setProperty(
                    "resilience4j.ratelimiter.instances." + system + ".baseConfig", "custom");
        }

        new IntegrationResilienceEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(
                        environment.getProperty(
                                "resilience4j.circuitbreaker.configs.telemedicine-external.slidingWindowSize"))
                .isEqualTo("99");
    }
}
