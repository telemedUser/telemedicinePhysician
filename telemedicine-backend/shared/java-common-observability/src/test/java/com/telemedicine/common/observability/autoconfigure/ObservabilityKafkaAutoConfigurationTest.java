package com.telemedicine.common.observability.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.observability.mdc.MdcKafkaConsumerInterceptor;
import com.telemedicine.common.observability.mdc.MdcKafkaProducerInterceptor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.RecordInterceptor;

class ObservabilityKafkaAutoConfigurationTest {

    @Test
    void registersKafkaMdcBeansWhenKafkaIsPresent() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestMeterRegistryConfiguration.class)
                .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class))
                .withPropertyValues("telemedicine.observability.enabled=true")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(MdcKafkaProducerInterceptor.class);
                            assertThat(context).hasSingleBean(MdcKafkaConsumerInterceptor.class);
                            assertThat(context).hasSingleBean(RecordInterceptor.class);
                        });
    }

    @Configuration
    static class TestMeterRegistryConfiguration {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
