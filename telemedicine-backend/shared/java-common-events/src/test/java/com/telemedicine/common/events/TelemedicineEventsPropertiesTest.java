package com.telemedicine.common.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TelemedicineEventsPropertiesTest {

    @Test
    void defaultsMatchPhase0() {
        TelemedicineEventsProperties properties = new TelemedicineEventsProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBootstrapServers()).isEqualTo("kafka:9092");
        assertThat(properties.getProducer().getAcks()).isEqualTo("all");
        assertThat(properties.getProducer().getCompressionType()).isEqualTo("zstd");
        assertThat(properties.getConsumer().getMaxRetries()).isEqualTo(3);
        assertThat(properties.getConsumer().getProcessedEventTtl()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void everySettingIsWritable() {
        TelemedicineEventsProperties properties = new TelemedicineEventsProperties();
        properties.setEnabled(false);
        properties.setBootstrapServers("localhost:9093");
        properties.setSource("custom");
        properties.getProducer().setAcks("1");
        properties.getProducer().setRetries(1);
        properties.getProducer().setMaxInFlight(1);
        properties.getProducer().setCompressionType("gzip");
        properties.getProducer().setDeliveryTimeout(Duration.ofSeconds(30));
        properties.getConsumer().setConcurrency(2);
        properties.getConsumer().setMaxRetries(5);
        properties.getConsumer().setInitialRetryInterval(Duration.ofMillis(500));
        properties.getConsumer().setMaxRetryInterval(Duration.ofSeconds(10));
        properties.getConsumer().setRetryMultiplier(1.5);
        properties.getConsumer().setProcessedEventTtl(Duration.ofDays(1));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBootstrapServers()).isEqualTo("localhost:9093");
        assertThat(properties.getSource()).isEqualTo("custom");
        assertThat(properties.getProducer().getCompressionType()).isEqualTo("gzip");
        assertThat(properties.getConsumer().getConcurrency()).isEqualTo(2);
    }
}
