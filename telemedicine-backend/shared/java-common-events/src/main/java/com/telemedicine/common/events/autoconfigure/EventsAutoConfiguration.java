package com.telemedicine.common.events.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.EventPublisher;
import com.telemedicine.common.events.TelemedicineEventsProperties;
import com.telemedicine.common.events.TopicNames;
import com.telemedicine.common.events.consumer.IdempotentConsumerAspect;
import com.telemedicine.common.events.consumer.InMemoryProcessedEventStore;
import com.telemedicine.common.events.consumer.ProcessedEventStore;
import com.telemedicine.common.events.kafka.KafkaEventPublisher;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

/** Registers the shared Kafka producer, consumer defaults and idempotent consumption. */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "telemedicine.events", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TelemedicineEventsProperties.class)
public class EventsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EventsAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "eventObjectMapper")
    public ObjectMapper eventObjectMapper() {
        return EventObjectMappers.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, byte[]> telemedicineEventProducerFactory(
            TelemedicineEventsProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, properties.getProducer().getAcks());
        config.put(ProducerConfig.RETRIES_CONFIG, properties.getProducer().getRetries());
        config.put(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                properties.getProducer().getMaxInFlight());
        config.put(
                ProducerConfig.COMPRESSION_TYPE_CONFIG,
                properties.getProducer().getCompressionType());
        config.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                (int) properties.getProducer().getDeliveryTimeout().toMillis());
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @ConditionalOnMissingBean(name = "telemedicineEventKafkaTemplate")
    public KafkaTemplate<String, byte[]> telemedicineEventKafkaTemplate(
            ProducerFactory<String, byte[]> telemedicineEventProducerFactory) {
        return new KafkaTemplate<>(telemedicineEventProducerFactory);
    }

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher telemedicineEventPublisher(
            KafkaTemplate<String, byte[]> telemedicineEventKafkaTemplate,
            ObjectMapper eventObjectMapper,
            @Value("${telemedicine.events.source:${spring.application.name:telemedicine}}")
                    String source) {
        return new KafkaEventPublisher(telemedicineEventKafkaTemplate, eventObjectMapper, source);
    }

    @Bean
    @ConditionalOnMissingBean(ProcessedEventStore.class)
    public ProcessedEventStore telemedicineProcessedEventStore() {
        log.warn(
                "No ProcessedEventStore bean found; falling back to the in-memory store."
                        + " Duplicate events may be processed again after a restart. Add"
                        + " java-common-persistence for the JPA implementation.");
        return new InMemoryProcessedEventStore();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    static class IdempotencyConfiguration {

        @Bean
        @ConditionalOnMissingBean
        IdempotentConsumerAspect telemedicineIdempotentConsumerAspect(
                ProcessedEventStore store,
                ObjectMapper eventObjectMapper,
                TelemedicineEventsProperties properties,
                Environment environment) {
            return new IdempotentConsumerAspect(store, eventObjectMapper, properties, environment);
        }
    }

    @Bean
    @ConditionalOnMissingBean(name = "telemedicineKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, byte[]>
            telemedicineKafkaListenerContainerFactory(
                    TelemedicineEventsProperties properties,
                    KafkaTemplate<String, byte[]> telemedicineEventKafkaTemplate) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, ByteArrayDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        ConsumerFactory<String, byte[]> consumerFactory = new DefaultKafkaConsumerFactory<>(config);

        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(properties.getConsumer().getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        telemedicineEventKafkaTemplate,
                        (record, exception) ->
                                new org.apache.kafka.common.TopicPartition(
                                        TopicNames.dlqForTopic(record.topic()),
                                        record.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(properties.getConsumer().getInitialRetryInterval().toMillis());
        backOff.setMaxInterval(properties.getConsumer().getMaxRetryInterval().toMillis());
        backOff.setMultiplier(properties.getConsumer().getRetryMultiplier());
        backOff.setMaxAttempts(properties.getConsumer().getMaxRetries());

        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, backOff));
        return factory;
    }
}
