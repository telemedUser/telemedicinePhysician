package com.telemedicine.notificationservice.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.payload.StatusChanged;
import com.telemedicine.notificationservice.service.NotificationService;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class NotificationDomainEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private Acknowledgment acknowledgment;

    private NotificationDomainEventListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new NotificationDomainEventListener(objectMapper, notificationService);
    }

    @Test
    void statusChangedToOpenDoctorDispatchesPhysicianReady() throws Exception {
        EventEnvelope<StatusChanged> envelope =
                EventEnvelope.<StatusChanged>builder()
                        .eventId("evt-1")
                        .type("StatusChanged")
                        .payload(
                                new StatusChanged(
                                        "apt-1",
                                        "CHECKED_IN",
                                        "OPEN_DOCTOR",
                                        "phy-1",
                                        Instant.parse("2026-06-01T12:00:00Z")))
                        .build();
        byte[] bytes = objectMapper.writeValueAsBytes(envelope);

        listener.onStatusChanged(new ConsumerRecord<>("StatusChanged", 0, 0, "apt-1", bytes), acknowledgment);

        verify(notificationService).dispatchEvent(any(), any(), any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void statusChangedIgnoresNonOpenDoctor() throws Exception {
        EventEnvelope<StatusChanged> envelope =
                EventEnvelope.<StatusChanged>builder()
                        .eventId("evt-2")
                        .type("StatusChanged")
                        .payload(
                                new StatusChanged(
                                        "apt-1",
                                        "OPEN_PATIENT",
                                        "CHECKED_IN",
                                        "pat-1",
                                        Instant.parse("2026-06-01T12:00:00Z")))
                        .build();
        byte[] bytes = objectMapper.writeValueAsBytes(envelope);

        listener.onStatusChanged(new ConsumerRecord<>("StatusChanged", 0, 0, "apt-1", bytes), acknowledgment);

        verifyNoInteractions(notificationService);
        verify(acknowledgment).acknowledge();
    }
}
