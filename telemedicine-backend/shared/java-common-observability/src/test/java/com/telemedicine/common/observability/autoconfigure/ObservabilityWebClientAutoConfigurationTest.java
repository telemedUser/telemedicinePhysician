package com.telemedicine.common.observability.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ObservabilityWebClientAutoConfigurationTest {

    @Test
    void registersWebClientCustomizerWhenWebFluxPresent() {
        new ReactiveWebApplicationContextRunner()
                .withUserConfiguration(TestMeterRegistryConfiguration.class)
                .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
                .withPropertyValues("telemedicine.observability.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(WebClientCustomizer.class));
    }

    @Configuration
    static class TestMeterRegistryConfiguration {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
