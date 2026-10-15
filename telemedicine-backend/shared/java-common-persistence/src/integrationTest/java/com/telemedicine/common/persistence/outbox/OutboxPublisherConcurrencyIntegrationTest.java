package com.telemedicine.common.persistence.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.time.FixedTimeProvider;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.outbox.OutboxEvent;
import com.telemedicine.common.persistence.TelemedicineOutboxProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@EntityScan(basePackages = "com.telemedicine.common.events.outbox")
@EnableJpaRepositories(basePackages = "com.telemedicine.common.persistence.outbox")
class OutboxPublisherConcurrencyIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("telemedicine")
                    .withUsername("telemedicine")
                    .withPassword("telemedicine");

    private static ExecutorService executor;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeAll
    static void startExecutor() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterAll
    static void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Autowired private OutboxEventRepository repository;

    @Autowired private PlatformTransactionManager transactionManager;

    private KafkaTemplate<String, String> kafkaTemplate;
    private AtomicInteger publishCount;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetMocks() {
        publishCount = new AtomicInteger();
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            publishCount.incrementAndGet();
                            return CompletableFuture.completedFuture(mock(SendResult.class));
                        });
    }

    @Test
    void twoPublishersDoNotDoublePublishTheSameEvent() {
        OutboxEvent event = persistPendingEvent();
        OutboxPublisher publisherA = createPublisher();
        OutboxPublisher publisherB = createPublisher();

        executor.submit(publisherA::publishPending);
        executor.submit(publisherB::publishPending);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(publishCount.get()).isEqualTo(1));

        OutboxEvent reloaded = repository.findById(event.getId()).orElseThrow();
        assertThat(reloaded.isPublished()).isTrue();
    }

    private OutboxPublisher createPublisher() {
        TelemedicineOutboxProperties properties = new TelemedicineOutboxProperties();
        properties.setBatchSize(10);
        ObjectMapper objectMapper = EventObjectMappers.create();
        return new OutboxPublisher(
                repository,
                kafkaTemplate,
                objectMapper,
                properties,
                new FixedTimeProvider(NOW),
                transactionManager,
                new SimpleMeterRegistry());
    }

    private OutboxEvent persistPendingEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId("01JCONCURRENT");
        event.setAggregateType("Appointment");
        event.setAggregateId("APT-1");
        event.setEventType(EventTypes.LINK_GENERATED);
        event.setEventVersion("1.0");
        event.setTopic(EventTypes.LINK_GENERATED);
        event.setSubject("APT-1");
        event.setCreatedAt(NOW);
        event.setPublished(false);
        event.setFailed(false);
        event.setAttempts(0);
        event.setPayload(
                EventObjectMappers.create().createObjectNode().put("appointmentId", "APT-1"));
        return repository.saveAndFlush(event);
    }
}
