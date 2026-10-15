package com.telemedicine.common.persistence.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.outbox.OutboxEvent;
import com.telemedicine.common.events.payload.LinkGenerated;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxRecorderTest {

    @Mock private OutboxEventRepository repository;

    @Test
    void storesJsonPayloadCompatibleWithPublisher() {
        OutboxRecorder recorder = new OutboxRecorder(repository, EventObjectMappers.create());
        LinkGenerated payload =
                new LinkGenerated("APT-1", "LNK-1", Instant.parse("2026-06-01T12:00:00Z"), "SMS");

        recorder.record("Appointment", "APT-1", payload, "01JCORR");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        JsonNode node = saved.getPayload();
        assertThat(saved.getEventType()).isEqualTo(EventTypes.LINK_GENERATED);
        assertThat(node.get("appointmentId").asText()).isEqualTo("APT-1");
        assertThat(saved.isPublished()).isFalse();
        assertThat(saved.getAttempts()).isZero();
    }
}
