package com.telemedicine.surveyservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.ConsultationClosed;
import com.telemedicine.surveyservice.service.SurveyService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class SurveyDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(SurveyDomainEventListener.class);

    private final ObjectMapper eventObjectMapper;
    private final SurveyService surveyService;

    public SurveyDomainEventListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper, SurveyService surveyService) {
        this.eventObjectMapper = eventObjectMapper;
        this.surveyService = surveyService;
    }

    @DomainEventListener(EventTypes.CONSULTATION_CLOSED)
    @IdempotentConsumer
    public void onConsultationClosed(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<ConsultationClosed> envelope = readEnvelope(record.value(), ConsultationClosed.class);
        ConsultationClosed payload = envelope.payload();
        if (!"COMPLETED".equalsIgnoreCase(payload.outcome())) {
            acknowledgment.acknowledge();
            return;
        }
        surveyService.requestSurveyForAppointment(payload.appointmentId());
        acknowledgment.acknowledge();
        log.debug("Created survey request for appointment {}", payload.appointmentId());
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
