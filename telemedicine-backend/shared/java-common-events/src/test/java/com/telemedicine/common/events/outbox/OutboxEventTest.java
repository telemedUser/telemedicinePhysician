package com.telemedicine.common.events.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.EventObjectMappers;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    private final ObjectMapper objectMapper = EventObjectMappers.create();

    @Test
    void gettersAndSettersRoundTrip() {
        OutboxEvent event = new OutboxEvent();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        event.setId("01J");
        event.setAggregateType("Appointment");
        event.setAggregateId("APT-1");
        event.setEventType("LinkGenerated");
        event.setEventVersion("1.0");
        event.setTopic("LinkGenerated");
        event.setCorrelationId("01JCORR");
        event.setCausationId("01JCAUSE");
        event.setSubject("APT-1");
        event.setPayload(objectMapper.createObjectNode().put("eventId", "01J"));
        event.setCreatedAt(now);
        event.setPublishedAt(now.plusSeconds(1));
        event.setPublished(true);
        event.setAttempts(2);
        event.setLastError("timeout");
        event.setNextAttemptAt(now.plusSeconds(30));
        event.setFailed(false);

        assertThat(event.getId()).isEqualTo("01J");
        assertThat(event.getEventId()).isEqualTo("01J");
        assertThat(event.getAggregateType()).isEqualTo("Appointment");
        assertThat(event.getAggregateId()).isEqualTo("APT-1");
        assertThat(event.getEventType()).isEqualTo("LinkGenerated");
        assertThat(event.getEventVersion()).isEqualTo("1.0");
        assertThat(event.getTopic()).isEqualTo("LinkGenerated");
        assertThat(event.getCorrelationId()).isEqualTo("01JCORR");
        assertThat(event.getCausationId()).isEqualTo("01JCAUSE");
        assertThat(event.getSubject()).isEqualTo("APT-1");
        assertThat(event.getPayload().get("eventId").asText()).isEqualTo("01J");
        assertThat(event.getPayloadJson()).isEqualTo(event.getPayload());
        assertThat(event.getCreatedAt()).isEqualTo(now);
        assertThat(event.getOccurredAt()).isEqualTo(now);
        assertThat(event.getPublishedAt()).isEqualTo(now.plusSeconds(1));
        assertThat(event.isPublished()).isTrue();
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getLastError()).isEqualTo("timeout");
        assertThat(event.getNextAttemptAt()).isEqualTo(now.plusSeconds(30));
        assertThat(event.isFailed()).isFalse();
    }
}
