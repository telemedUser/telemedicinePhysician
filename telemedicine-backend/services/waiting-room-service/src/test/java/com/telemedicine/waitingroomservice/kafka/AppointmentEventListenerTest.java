package com.telemedicine.waitingroomservice.kafka;

import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.payload.AppointmentCreated;
import com.telemedicine.waitingroomservice.status.StatusService;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class AppointmentEventListenerTest {

    @Mock private StatusService statusService;
    @Mock private Acknowledgment acknowledgment;

    private AppointmentEventListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new AppointmentEventListener(objectMapper, statusService);
    }

    @Test
    void appointmentCreatedInitializesWaitingRoom() throws Exception {
        AppointmentCreated created =
                new AppointmentCreated(
                        "apt-1", "pat-ref", "phy-ref", Instant.parse("2026-06-01T14:00:00Z"), "VIDEO", "HIS");
        EventEnvelope<AppointmentCreated> envelope =
                EventEnvelope.<AppointmentCreated>builder()
                        .eventId("evt-1")
                        .type("AppointmentCreated")
                        .subject("apt-1")
                        .payload(created)
                        .build();
        byte[] bytes = objectMapper.writeValueAsBytes(envelope);

        listener.onAppointmentCreated(new ConsumerRecord<>("AppointmentCreated", 0, 0, "apt-1", bytes), acknowledgment);

        verify(statusService).initializeFromAppointmentCreated(created);
        verify(acknowledgment).acknowledge();
    }
}
