package com.telemedicine.common.events.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.payload.LinkGenerated;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KafkaEventPublisherIntegrationTest {

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Test
    void roundTripsAnEnvelopeWithCloudEventsHeaders() {
        Map<String, Object> producerConfig =
                new HashMap<>(KafkaTestUtils.producerProps(KAFKA.getBootstrapServers()));
        KafkaTemplate<String, byte[]> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfig));

        KafkaEventPublisher publisher =
                new KafkaEventPublisher(
                        template, EventObjectMappers.create(), "waiting-room-service");

        LinkGenerated payload =
                new LinkGenerated("APT-1", "LNK-1", Instant.parse("2026-06-01T12:00:00Z"), "SMS");
        EventEnvelope<LinkGenerated> envelope =
                EventEnvelope.<LinkGenerated>builder()
                        .eventId("01JTESTEVENT")
                        .type(EventTypes.LINK_GENERATED)
                        .subject("APT-1")
                        .correlationId("01JCORR")
                        .payload(payload)
                        .build();

        publisher.publish(envelope).join();

        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfig.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerConfig)) {
            consumer.subscribe(java.util.List.of(EventTypes.LINK_GENERATED));

            await().atMost(Duration.ofSeconds(20))
                    .untilAsserted(
                            () -> {
                                ConsumerRecord<String, byte[]> record =
                                        KafkaTestUtils.getSingleRecord(
                                                consumer, EventTypes.LINK_GENERATED);
                                assertThat(record.key()).isEqualTo("APT-1");
                                assertThat(new String(record.headers().lastHeader("ce_id").value()))
                                        .isEqualTo("01JTESTEVENT");
                                assertThat(
                                                new String(
                                                        record.headers()
                                                                .lastHeader("ce_type")
                                                                .value()))
                                        .isEqualTo(EventTypes.LINK_GENERATED);
                                assertThat(new String(record.value())).contains("01JTESTEVENT");
                            });
        }
    }
}
