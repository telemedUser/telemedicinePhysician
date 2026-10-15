package com.telemedicine.waitingroomservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.IdentityValidated;
import com.telemedicine.waitingroomservice.status.StatusService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class WaitingRoomDomainEventListener {

    private final ObjectMapper eventObjectMapper;
    private final StatusService statusService;

    public WaitingRoomDomainEventListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            StatusService statusService) {
        this.eventObjectMapper = eventObjectMapper;
        this.statusService = statusService;
    }

    @DomainEventListener(EventTypes.IDENTITY_VALIDATED)
    @IdempotentConsumer
    public void onIdentityValidated(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<IdentityValidated> envelope = readEnvelope(record.value(), IdentityValidated.class);
        IdentityValidated payload = envelope.payload();
        statusService.markIdentityValidated(payload.appointmentId(), payload.patientRef());
        acknowledgment.acknowledge();
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
