package com.telemedicine.physicianservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.DeviceTestCompleted;
import com.telemedicine.common.events.payload.PatientEnteredWaitingRoom;
import com.telemedicine.common.events.payload.StatusChanged;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemEntity;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemRepository;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Maintains the worklist read model (PRD §12.3) from waiting-room events. Each handler is
 * idempotent and tolerant of out-of-order delivery (TM-INT-013): it upserts the item and only
 * advances fields it owns. Waiting-room changes must reflect on the worklist within 3 seconds
 * (TM-WLT-006, TM-NFR-003).
 */
@Component
public class WorklistProjectionListener {

    private static final Logger log = LoggerFactory.getLogger(WorklistProjectionListener.class);
    private static final String DEFAULT_MARKET = "CL";

    private final ObjectMapper eventObjectMapper;
    private final WorklistItemRepository worklistItemRepository;

    public WorklistProjectionListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            WorklistItemRepository worklistItemRepository) {
        this.eventObjectMapper = eventObjectMapper;
        this.worklistItemRepository = worklistItemRepository;
    }

    @DomainEventListener(EventTypes.PATIENT_ENTERED_WAITING_ROOM)
    @IdempotentConsumer
    public void onPatientEntered(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<PatientEnteredWaitingRoom> envelope =
                readEnvelope(record.value(), PatientEnteredWaitingRoom.class);
        PatientEnteredWaitingRoom payload = envelope.payload();
        WorklistItemEntity item = findOrCreate(payload.appointmentId());
        item.setPatientRef(payload.patientRef());
        item.setEnteredAt(payload.enteredAt());
        item.setReadiness("IN_PROGRESS");
        save(item);
        ack.acknowledge();
    }

    @DomainEventListener(EventTypes.DEVICE_TEST_COMPLETED)
    @IdempotentConsumer
    public void onDeviceTest(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<DeviceTestCompleted> envelope =
                readEnvelope(record.value(), DeviceTestCompleted.class);
        DeviceTestCompleted payload = envelope.payload();
        WorklistItemEntity item = findOrCreate(payload.appointmentId());
        boolean ready = payload.cameraOk() && payload.microphoneOk() && payload.bandwidthKbps() > 0;
        item.setReadiness(ready ? "READY" : "NOT_READY");
        item.setTechnicalStatus(ready ? "OK" : "IMPAIRED");
        save(item);
        ack.acknowledge();
    }

    @DomainEventListener(EventTypes.STATUS_CHANGED)
    @IdempotentConsumer
    public void onStatusChanged(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<StatusChanged> envelope = readEnvelope(record.value(), StatusChanged.class);
        StatusChanged payload = envelope.payload();
        WorklistItemEntity item = findOrCreate(payload.appointmentId());
        if (payload.toStatus() != null) {
            item.setConsultationStatus(payload.toStatus().toUpperCase(java.util.Locale.ROOT));
        }
        save(item);
        ack.acknowledge();
    }

    private WorklistItemEntity findOrCreate(String appointmentId) {
        return worklistItemRepository
                .findById(appointmentId)
                .orElseGet(() -> new WorklistItemEntity(appointmentId, DEFAULT_MARKET));
    }

    private void save(WorklistItemEntity item) {
        item.setUpdatedAt(Instant.now());
        worklistItemRepository.save(item);
        log.debug("Worklist item {} updated", item.getAppointmentId());
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
