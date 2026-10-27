package com.telemedicine.physicianservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.AppointmentCreated;
import com.telemedicine.common.events.payload.ConsultationClosed;
import com.telemedicine.common.events.payload.ConsultationContextPublished;
import com.telemedicine.common.events.payload.DeviceTestCompleted;
import com.telemedicine.common.events.payload.PatientEnteredWaitingRoom;
import com.telemedicine.common.events.payload.StatusChanged;
import com.telemedicine.physicianservice.domain.detail.PatientDetailEntity;
import com.telemedicine.physicianservice.domain.detail.PatientDetailRepository;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemEntity;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemRepository;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the worklist read model (PRD §12.3) from waiting-room events. Each handler is
 * idempotent and tolerant of out-of-order delivery (TM-INT-013): it upserts the item and only
 * advances fields it owns. Waiting-room changes must reflect on the worklist within 3 seconds
 * (TM-WLT-006, TM-NFR-003).
 */
@Component
public class WorklistProjectionListener {

    private static final Logger log = LoggerFactory.getLogger(WorklistProjectionListener.class);
    private static final String DEFAULT_MARKET = "IN";

    private final ObjectMapper eventObjectMapper;
    private final WorklistItemRepository worklistItemRepository;
    private final PatientDetailRepository patientDetailRepository;

    public WorklistProjectionListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            WorklistItemRepository worklistItemRepository,
            PatientDetailRepository patientDetailRepository) {
        this.eventObjectMapper = eventObjectMapper;
        this.worklistItemRepository = worklistItemRepository;
        this.patientDetailRepository = patientDetailRepository;
    }

    /**
     * A patient booking was created (by patient-booking-service). Seeds the worklist item so the
     * booked patient appears on the assigned physician's worklist before they enter the waiting
     * room. The item is owned by the physician in {@code physicianRef}; later waiting-room and
     * quality events advance its readiness and status.
     */
    @DomainEventListener(EventTypes.APPOINTMENT_CREATED)
    @IdempotentConsumer
    public void onAppointmentCreated(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<AppointmentCreated> envelope =
                readEnvelope(record.value(), AppointmentCreated.class);
        AppointmentCreated payload = envelope.payload();
        WorklistItemEntity item = findOrCreate(payload.appointmentId());
        if (payload.physicianRef() != null) {
            item.setProviderId(payload.physicianRef());
        }
        item.setPatientRef(payload.patientRef());
        if (payload.modality() != null) {
            item.setMode(payload.modality().toLowerCase(java.util.Locale.ROOT));
        }
        if (payload.scheduledAt() != null) {
            item.setScheduledAt(payload.scheduledAt());
        }
        item.setConsultationStatus("SCHEDULED");
        if (item.getFlags() == null || item.getFlags().isBlank()) {
            item.setFlags("[\"FIRST_VISIT\"]");
        }
        save(item);
        ack.acknowledge();
    }

    @DomainEventListener(EventTypes.PATIENT_ENTERED_WAITING_ROOM)
    @IdempotentConsumer
    @Transactional
    public void onPatientEntered(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<PatientEnteredWaitingRoom> envelope =
                readEnvelope(record.value(), PatientEnteredWaitingRoom.class);
        PatientEnteredWaitingRoom payload = envelope.payload();
        Instant enteredAt = payload.enteredAt() != null ? payload.enteredAt() : Instant.now();

        // Stamp arrival with an atomic UPDATE so it cannot be clobbered by the AppointmentCreated /
        // DeviceTestCompleted handlers running concurrently on other consumer threads (they share
        // the row; a read-modify-write would lose this write under @Version optimistic locking).
        int updated = worklistItemRepository.markEntered(
                payload.appointmentId(), enteredAt, payload.patientRef(), Instant.now());
        if (updated == 0) {
            // Row not created yet (AppointmentCreated hasn't been consumed) — create it now.
            WorklistItemEntity item = findOrCreate(payload.appointmentId());
            item.setPatientRef(payload.patientRef());
            item.setEnteredAt(enteredAt);
            if (!"READY".equals(item.getReadiness())) {
                item.setReadiness("IN_PROGRESS");
            }
            save(item);
        }
        log.info("Worklist arrival stamped for {} (enteredAt={})", payload.appointmentId(), enteredAt);
        ack.acknowledge();
    }

    @DomainEventListener(EventTypes.DEVICE_TEST_COMPLETED)
    @IdempotentConsumer
    @Transactional
    public void onDeviceTest(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<DeviceTestCompleted> envelope =
                readEnvelope(record.value(), DeviceTestCompleted.class);
        DeviceTestCompleted payload = envelope.payload();
        boolean ready = payload.cameraOk() && payload.microphoneOk() && payload.bandwidthKbps() > 0;
        String readiness = ready ? "READY" : "NOT_READY";
        String tech = ready ? "OK" : "IMPAIRED";
        // Atomic update so it does not clobber the concurrent arrival stamp (enteredAt) on the same
        // row (events for one join arrive on independent topics / consumer threads — TM-INT-013).
        int updated = worklistItemRepository.markDeviceTest(payload.appointmentId(), readiness, tech, Instant.now());
        if (updated == 0) {
            WorklistItemEntity item = findOrCreate(payload.appointmentId());
            item.setReadiness(readiness);
            item.setTechnicalStatus(tech);
            save(item);
        }
        ack.acknowledge();
    }

    @DomainEventListener(EventTypes.STATUS_CHANGED)
    @IdempotentConsumer
    @Transactional
    public void onStatusChanged(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<StatusChanged> envelope = readEnvelope(record.value(), StatusChanged.class);
        StatusChanged payload = envelope.payload();
        if (payload.toStatus() != null) {
            String status = payload.toStatus().toUpperCase(java.util.Locale.ROOT);
            int updated = worklistItemRepository.markStatus(payload.appointmentId(), status, Instant.now());
            if (updated == 0) {
                WorklistItemEntity item = findOrCreate(payload.appointmentId());
                item.setConsultationStatus(status);
                save(item);
            }
        }
        ack.acknowledge();
    }

    /**
     * The patient-booking-service published the review context for an appointment (prototype D-03).
     * Projects it into the {@code patient_detail} read model that backs the physician detail page.
     * Last-writer-wins on appointmentId; stores only what the event carries.
     */
    /**
     * The physician closed the consultation (D-10). Flip the worklist row to CLOSED so Home and the
     * Worklist show "Completed". Atomic status update so it cannot be clobbered by a racing event.
     */
    @DomainEventListener(EventTypes.CONSULTATION_CLOSED)
    @IdempotentConsumer
    @Transactional
    public void onConsultationClosed(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<ConsultationClosed> envelope = readEnvelope(record.value(), ConsultationClosed.class);
        ConsultationClosed payload = envelope.payload();
        int updated = worklistItemRepository.markStatus(payload.appointmentId(), "CLOSED", Instant.now());
        if (updated == 0) {
            WorklistItemEntity item = findOrCreate(payload.appointmentId());
            item.setConsultationStatus("CLOSED");
            save(item);
        }
        log.info("Worklist marked CLOSED for {}", payload.appointmentId());
        ack.acknowledge();
    }

    @DomainEventListener(EventTypes.CONSULTATION_CONTEXT_PUBLISHED)
    @IdempotentConsumer
    public void onConsultationContext(ConsumerRecord<String, byte[]> record, Acknowledgment ack)
            throws Exception {
        EventEnvelope<ConsultationContextPublished> envelope =
                readEnvelope(record.value(), ConsultationContextPublished.class);
        ConsultationContextPublished p = envelope.payload();

        PatientDetailEntity item =
                patientDetailRepository
                        .findById(p.appointmentId())
                        .orElseGet(() -> new PatientDetailEntity(p.appointmentId(), DEFAULT_MARKET));
        item.setPatientDisplay(p.patientDisplay());
        item.setAgeYears(p.ageYears());
        item.setSexCode(p.sexCode());
        item.setCity(p.city());
        item.setInsuranceScheme(p.insuranceScheme());
        item.setNationalIdMasked(p.nationalIdMasked());
        item.setModality(p.modality());
        item.setVisitType(p.visitType());
        item.setScheduledAt(p.scheduledAt());
        item.setSeverityScore(p.severityScore());
        item.setSymptomDurationCode(p.symptomDurationCode());
        item.setSymptomCount(p.symptomCount());
        item.setAttachmentCount(p.attachmentCount());
        item.setRedFlag(p.redFlag());
        item.setConsultationFeeMinor(p.consultationFeeMinor());
        item.setVoucherMinor(p.voucherMinor());
        item.setAmountPaidMinor(p.amountPaidMinor());
        item.setCurrency(p.currency() != null ? p.currency() : "INR");
        item.setPaymentStatus(p.paymentStatus());
        item.setContextJson(p.clinicalContextJson());
        item.setUpdatedAt(Instant.now());
        patientDetailRepository.save(item);
        log.debug("Patient detail projected for {}", p.appointmentId());
        ack.acknowledge();
    }

    private WorklistItemEntity findOrCreate(String appointmentId) {
        return worklistItemRepository
                .findById(appointmentId)
                .orElseGet(() -> new WorklistItemEntity(appointmentId, DEFAULT_MARKET));
    }

    /**
     * Persist the projected item, tolerating the insert race where two events for the same new
     * appointment are consumed back to back: if a concurrent insert already created the row, re-read
     * it, copy the fields this handler set, and save again as an update. Keeps each handler
     * idempotent and order-tolerant (TM-INT-013).
     */
    private void save(WorklistItemEntity item) {
        item.setUpdatedAt(Instant.now());
        try {
            worklistItemRepository.saveAndFlush(item);
        } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
            WorklistItemEntity existing =
                    worklistItemRepository.findById(item.getAppointmentId()).orElse(null);
            if (existing == null) {
                throw duplicate;
            }
            copyInto(item, existing);
            existing.setUpdatedAt(Instant.now());
            worklistItemRepository.saveAndFlush(existing);
            log.debug("Worklist item {} merged after insert race", item.getAppointmentId());
            return;
        }
        log.debug("Worklist item {} updated", item.getAppointmentId());
    }

    /** Copy the mutable projection fields from a freshly-built item onto the persisted row. */
    private void copyInto(WorklistItemEntity from, WorklistItemEntity to) {
        if (from.getProviderId() != null) {
            to.setProviderId(from.getProviderId());
        }
        if (from.getPatientRef() != null) {
            to.setPatientRef(from.getPatientRef());
        }
        if (from.getMode() != null) {
            to.setMode(from.getMode());
        }
        if (from.getEnteredAt() != null) {
            to.setEnteredAt(from.getEnteredAt());
        }
        if (from.getScheduledAt() != null) {
            to.setScheduledAt(from.getScheduledAt());
        }
        // Readiness only moves forward: a persisted READY (device check passed) is not regressed by
        // a racing waiting-room event that merges IN_PROGRESS/NOT_READY on top of it.
        if (!"READY".equals(to.getReadiness())) {
            to.setReadiness(from.getReadiness());
        }
        to.setConsultationStatus(from.getConsultationStatus());
        to.setTechnicalStatus(from.getTechnicalStatus());
        to.setTelemetryAvailable(from.isTelemetryAvailable());
        if (from.getFlags() != null) {
            to.setFlags(from.getFlags());
        }
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
