package com.telemedicine.common.events.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.events.EventPublisher;
import com.telemedicine.common.events.TelemedicineEventsProperties;
import com.telemedicine.common.events.consumer.ProcessedEventStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EventsAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    KafkaAutoConfiguration.class, EventsAutoConfiguration.class))
                    .withPropertyValues(
                            "telemedicine.events.bootstrap-servers=localhost:9092",
                            "spring.application.name=waiting-room-service");

    @Test
    void factoryIsConfiguredWithManualAckAndErrorHandler() {
        runner.run(
                context -> {
                    org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?>
                            factory =
                                    context.getBean(
                                            "telemedicineKafkaListenerContainerFactory",
                                            org.springframework.kafka.config
                                                    .ConcurrentKafkaListenerContainerFactory.class);
                    assertThat(factory.getContainerProperties().getAckMode())
                            .isEqualTo(
                                    org.springframework.kafka.listener.ContainerProperties.AckMode
                                            .MANUAL);
                    assertThat(factory).isNotNull();
                });
    }

    @Test
    void registersPublisherAndSharedBeans() {
        runner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(EventPublisher.class)
                                .hasSingleBean(ProcessedEventStore.class)
                                .hasBean("eventObjectMapper")
                                .hasBean("telemedicineKafkaListenerContainerFactory"));
    }

    @Test
    void bindsProperties() {
        runner.withPropertyValues("telemedicine.events.producer.compression-type=lz4")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(TelemedicineEventsProperties.class)
                                                        .getProducer()
                                                        .getCompressionType())
                                        .isEqualTo("lz4"));
    }

    @Test
    void bindsConsumerConcurrency() {
        runner.withPropertyValues("telemedicine.events.consumer.concurrency=3")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(TelemedicineEventsProperties.class)
                                                        .getConsumer()
                                                        .getConcurrency())
                                        .isEqualTo(3));
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("telemedicine.events.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(EventPublisher.class));
    }
}
