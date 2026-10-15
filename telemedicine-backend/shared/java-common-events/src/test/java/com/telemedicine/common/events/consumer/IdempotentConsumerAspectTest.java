package com.telemedicine.common.events.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.TelemedicineEventsProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class IdempotentConsumerAspectTest {

    private final InMemoryProcessedEventStore store = new InMemoryProcessedEventStore();
    private final ObjectMapper objectMapper = EventObjectMappers.create();
    private final TelemedicineEventsProperties properties = new TelemedicineEventsProperties();
    private final MockEnvironment environment = new MockEnvironment();
    private final IdempotentConsumerAspect aspect =
            new IdempotentConsumerAspect(store, objectMapper, properties, environment);

    @Test
    void skipsProcessingWhenTheEventWasAlreadyHandled() throws Throwable {
        store.markProcessed("svc", "E1", EventTypes.LINK_GENERATED, Duration.ofHours(1));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs())
                .thenReturn(new Object[] {recordWithEvent("E1", EventTypes.LINK_GENERATED)});

        assertThat(aspect.skipDuplicates(joinPoint, annotation("svc"))).isNull();
        verify(joinPoint, never()).proceed();
    }

    @Test
    void proceedsAndRecordsFirstTimeEvents() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs())
                .thenReturn(new Object[] {recordWithEvent("E2", EventTypes.LINK_GENERATED)});
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.skipDuplicates(joinPoint, annotation("svc"))).isEqualTo("ok");
        assertThat(store.find("svc", "E2")).isPresent();
    }

    @Test
    void resolvesPlaceholderConsumerGroups() throws Throwable {
        environment.setProperty("spring.application.name", "waiting-room-service");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs())
                .thenReturn(new Object[] {recordWithEvent("E3", EventTypes.LINK_GENERATED)});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.skipDuplicates(joinPoint, annotation("${spring.application.name:telemedicine}"));

        assertThat(store.find("waiting-room-service", "E3")).isPresent();
    }

    @Test
    void resolvesPropertyPlaceholderWithoutDefault() throws Throwable {
        environment.setProperty("spring.application.name", "identity-service");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs())
                .thenReturn(new Object[] {recordWithEvent("E5", EventTypes.IDENTITY_VALIDATED)});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.skipDuplicates(joinPoint, annotation("${spring.application.name}"));

        assertThat(store.find("identity-service", "E5")).isPresent();
    }

    @Test
    void resolvesPlainGroupNamesWithoutPlaceholderSyntax() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs())
                .thenReturn(new Object[] {recordWithEvent("E4", EventTypes.LINK_GENERATED)});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.skipDuplicates(joinPoint, annotation("plain-group"));

        assertThat(store.find("plain-group", "E4")).isPresent();
    }

    @Test
    void readsEventIdFromStringArgument() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"01JSTRINGEVENTID0001"});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.skipDuplicates(joinPoint, annotation("svc"));

        assertThat(store.find("svc", "01JSTRINGEVENTID0001")).isPresent();
    }

    @Test
    void proceedsWhenNoEventIdCanBeResolved() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getArgs()).thenReturn(new Object[] {"not-an-id"});
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.skipDuplicates(joinPoint, annotation("svc"))).isEqualTo("ok");
        assertThat(store.find("svc", "not-an-id")).isEmpty();
    }

    @Test
    void readsEventIdFromJsonBytes() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        byte[] body =
                "{\"eventId\":\"01JFROMJSON\",\"type\":\"LinkGenerated\"}"
                        .getBytes(StandardCharsets.UTF_8);
        when(joinPoint.getArgs()).thenReturn(new Object[] {body});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.skipDuplicates(joinPoint, annotation("svc"));

        assertThat(store.find("svc", "01JFROMJSON")).isPresent();
    }

    private static IdempotentConsumer annotation(String group) {
        return new IdempotentConsumer() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return IdempotentConsumer.class;
            }

            @Override
            public String group() {
                return group;
            }
        };
    }

    private static ConsumerRecord<String, byte[]> recordWithEvent(String eventId, String type) {
        byte[] body =
                ("{\"eventId\":\"" + eventId + "\",\"type\":\"" + type + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader("ce_id", eventId.getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("ce_type", type.getBytes(StandardCharsets.UTF_8)));
        return new ConsumerRecord<>(
                "LinkGenerated",
                0,
                0L,
                System.currentTimeMillis(),
                TimestampType.CREATE_TIME,
                null,
                0,
                body.length,
                "APT-1",
                body,
                headers);
    }
}
