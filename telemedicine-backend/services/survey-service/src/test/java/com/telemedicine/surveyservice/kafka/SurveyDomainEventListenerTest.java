package com.telemedicine.surveyservice.kafka;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.payload.ConsultationClosed;
import com.telemedicine.surveyservice.service.SurveyService;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class SurveyDomainEventListenerTest {

    @Mock private SurveyService surveyService;
    @Mock private Acknowledgment acknowledgment;

    private SurveyDomainEventListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new SurveyDomainEventListener(objectMapper, surveyService);
    }

    @Test
    void consultationClosedCreatesSurveyRequest() throws Exception {
        EventEnvelope<ConsultationClosed> envelope =
                EventEnvelope.<ConsultationClosed>builder()
                        .eventId("evt-1")
                        .type("ConsultationClosed")
                        .payload(
                                new ConsultationClosed(
                                        "apt-1", "COMPLETED", 900L, Instant.parse("2026-06-01T12:00:00Z")))
                        .build();
        byte[] bytes = objectMapper.writeValueAsBytes(envelope);

        listener.onConsultationClosed(new ConsumerRecord<>("ConsultationClosed", 0, 0, "apt-1", bytes), acknowledgment);

        verify(surveyService).requestSurveyForAppointment("apt-1");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consultationClosedIgnoresNonCompletedOutcome() throws Exception {
        EventEnvelope<ConsultationClosed> envelope =
                EventEnvelope.<ConsultationClosed>builder()
                        .eventId("evt-2")
                        .type("ConsultationClosed")
                        .payload(
                                new ConsultationClosed(
                                        "apt-1", "NO_SHOW", 0L, Instant.parse("2026-06-01T12:00:00Z")))
                        .build();
        byte[] bytes = objectMapper.writeValueAsBytes(envelope);

        listener.onConsultationClosed(new ConsumerRecord<>("ConsultationClosed", 0, 0, "apt-1", bytes), acknowledgment);

        verifyNoInteractions(surveyService);
        verify(acknowledgment).acknowledge();
    }
}
