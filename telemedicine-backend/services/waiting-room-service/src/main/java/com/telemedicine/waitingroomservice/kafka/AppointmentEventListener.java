package com.telemedicine.waitingroomservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.AppointmentCreated;
import com.telemedicine.common.events.payload.AppointmentRescheduled;
import com.telemedicine.waitingroomservice.status.StatusService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    private final ObjectMapper eventObjectMapper;
    private final StatusService statusService;

    public AppointmentEventListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper, StatusService statusService) {
        this.eventObjectMapper = eventObjectMapper;
        this.statusService = statusService;
    }

    @DomainEventListener(EventTypes.APPOINTMENT_CREATED)
    @IdempotentConsumer
    public void onAppointmentCreated(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<AppointmentCreated> envelope = readEnvelope(record.value(), AppointmentCreated.class);
        statusService.initializeFromAppointmentCreated(envelope.payload());
        acknowledgment.acknowledge();
        log.debug("Initialized waiting room for appointment {}", envelope.payload().appointmentId());
    }

    @DomainEventListener(EventTypes.APPOINTMENT_RESCHEDULED)
    @IdempotentConsumer
    public void onAppointmentRescheduled(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<AppointmentRescheduled> envelope =
                readEnvelope(record.value(), AppointmentRescheduled.class);
        statusService.handleAppointmentRescheduled(envelope.payload());
        acknowledgment.acknowledge();
        log.debug("Updated waiting room schedule for appointment {}", envelope.payload().appointmentId());
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
