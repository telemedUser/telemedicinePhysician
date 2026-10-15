package com.telemedicine.common.persistence.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class PersistenceEnvironmentPostProcessorTest {

    private final PersistenceEnvironmentPostProcessor processor =
            new PersistenceEnvironmentPostProcessor();

    @Test
    void appliesOpenInViewAndHikariDefaults() {
        MockEnvironment environment = new MockEnvironment();
        SpringApplication application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size"))
                .isEqualTo("10");
        assertThat(environment.getProperty("spring.flyway.locations"))
                .contains("db/migration/common");
    }

    @Test
    void doesNotOverrideExplicitConsumerValues() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty("spring.jpa.open-in-view", "true")
                        .withProperty("spring.datasource.hikari.maximum-pool-size", "25");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("true");
        assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size"))
                .isEqualTo("25");
    }
}
