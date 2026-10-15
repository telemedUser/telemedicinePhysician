package com.telemedicine.common.observability.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.observability.filter.CorrelationIdFilter;
import com.telemedicine.common.observability.mdc.MdcPropagatingTaskDecorator;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestMeterRegistryConfiguration.class)
                    .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
                    .withPropertyValues(
                            "spring.application.name=template-java",
                            "telemedicine.observability.enabled=true");

    private final WebApplicationContextRunner webContextRunner =
            new WebApplicationContextRunner()
                    .withUserConfiguration(TestMeterRegistryConfiguration.class)
                    .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
                    .withPropertyValues(
                            "spring.application.name=template-java",
                            "telemedicine.observability.enabled=true");

    @Test
    void registersCoreBeans() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(JourneyMetrics.class);
                    assertThat(context).hasSingleBean(MdcPropagatingTaskDecorator.class);
                    assertThat(context).hasSingleBean(TaskDecorator.class);
                });
    }

    @Test
    void registersCorrelationFilterForServletApps() {
        webContextRunner.run(
                context -> assertThat(context).hasSingleBean(CorrelationIdFilter.class));
    }

    @Test
    void canBeDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
                .withPropertyValues("telemedicine.observability.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(JourneyMetrics.class));
    }

    @Configuration
    static class TestMeterRegistryConfiguration {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
