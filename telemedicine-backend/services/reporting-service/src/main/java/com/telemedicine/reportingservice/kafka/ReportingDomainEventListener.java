package com.telemedicine.reportingservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.ClinicalDocumentIssued;
import com.telemedicine.common.events.payload.ConnectionQualityDegraded;
import com.telemedicine.common.events.payload.ConsultationClosed;
import com.telemedicine.common.events.payload.IntegrationFailed;
import com.telemedicine.common.events.payload.MedicalLeaveIssued;
import com.telemedicine.common.events.payload.NotificationSent;
import com.telemedicine.common.events.payload.PatientEnteredWaitingRoom;
import com.telemedicine.common.events.payload.PaymentConfirmed;
import com.telemedicine.common.events.payload.PrescriptionIssued;
import com.telemedicine.common.events.payload.StatusChanged;
import com.telemedicine.common.events.payload.SurveyCompleted;
import com.telemedicine.common.events.payload.VideoSessionCreated;
import com.telemedicine.common.events.payload.VideoSessionStarted;
import com.telemedicine.reportingservice.service.KpiRecordingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ReportingDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReportingDomainEventListener.class);

    private final ObjectMapper eventObjectMapper;
    private final KpiRecordingService kpiRecordingService;

    public ReportingDomainEventListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            KpiRecordingService kpiRecordingService) {
        this.eventObjectMapper = eventObjectMapper;
        this.kpiRecordingService = kpiRecordingService;
    }

    @DomainEventListener(EventTypes.PATIENT_ENTERED_WAITING_ROOM)
    @IdempotentConsumer
    public void onPatientEnteredWaitingRoom(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<PatientEnteredWaitingRoom> envelope =
                readEnvelope(record.value(), PatientEnteredWaitingRoom.class);
        PatientEnteredWaitingRoom payload = envelope.payload();
        kpiRecordingService.recordWaitingRoomEntry(
                envelope.eventId(), payload.appointmentId(), payload.enteredAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.STATUS_CHANGED)
    @IdempotentConsumer
    public void onStatusChanged(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<StatusChanged> envelope = readEnvelope(record.value(), StatusChanged.class);
        StatusChanged payload = envelope.payload();
        if ("OPEN_DOCTOR".equalsIgnoreCase(payload.toStatus())) {
            kpiRecordingService.recordOpenDoctor(
                    envelope.eventId(), payload.appointmentId(), payload.changedAt());
        }
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.VIDEO_SESSION_CREATED)
    @IdempotentConsumer
    public void onVideoSessionCreated(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<VideoSessionCreated> envelope =
                readEnvelope(record.value(), VideoSessionCreated.class);
        VideoSessionCreated payload = envelope.payload();
        kpiRecordingService.recordVideoSessionCreated(
                envelope.eventId(), payload.appointmentId(), payload.createdAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.VIDEO_SESSION_STARTED)
    @IdempotentConsumer
    public void onVideoSessionStarted(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<VideoSessionStarted> envelope =
                readEnvelope(record.value(), VideoSessionStarted.class);
        VideoSessionStarted payload = envelope.payload();
        kpiRecordingService.recordVideoSessionStarted(
                envelope.eventId(), payload.appointmentId(), payload.startedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.PAYMENT_CONFIRMED)
    @IdempotentConsumer
    public void onPaymentConfirmed(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<PaymentConfirmed> envelope = readEnvelope(record.value(), PaymentConfirmed.class);
        PaymentConfirmed payload = envelope.payload();
        kpiRecordingService.recordPaymentConfirmed(
                envelope.eventId(), payload.appointmentId(), payload.confirmedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.CONSULTATION_CLOSED)
    @IdempotentConsumer
    public void onConsultationClosed(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<ConsultationClosed> envelope = readEnvelope(record.value(), ConsultationClosed.class);
        ConsultationClosed payload = envelope.payload();
        kpiRecordingService.recordConsultationClosed(
                envelope.eventId(), payload.appointmentId(), payload.outcome(), payload.closedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.CLINICAL_DOCUMENT_ISSUED)
    @IdempotentConsumer
    public void onClinicalDocumentIssued(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<ClinicalDocumentIssued> envelope =
                readEnvelope(record.value(), ClinicalDocumentIssued.class);
        ClinicalDocumentIssued payload = envelope.payload();
        kpiRecordingService.recordDocumentIssued(
                envelope.eventId(), payload.appointmentId(), payload.issuedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.SURVEY_COMPLETED)
    @IdempotentConsumer
    public void onSurveyCompleted(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<SurveyCompleted> envelope = readEnvelope(record.value(), SurveyCompleted.class);
        SurveyCompleted payload = envelope.payload();
        kpiRecordingService.recordSurveyCompleted(
                envelope.eventId(), payload.appointmentId(), payload.score(), payload.completedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.PRESCRIPTION_ISSUED)
    @IdempotentConsumer
    public void onPrescriptionIssued(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<PrescriptionIssued> envelope = readEnvelope(record.value(), PrescriptionIssued.class);
        PrescriptionIssued payload = envelope.payload();
        kpiRecordingService.recordPrescriptionIssued(
                envelope.eventId(), payload.appointmentId(), payload.issuedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.MEDICAL_LEAVE_ISSUED)
    @IdempotentConsumer
    public void onMedicalLeaveIssued(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<MedicalLeaveIssued> envelope = readEnvelope(record.value(), MedicalLeaveIssued.class);
        MedicalLeaveIssued payload = envelope.payload();
        kpiRecordingService.recordMedicalLeaveIssued(
                envelope.eventId(), payload.appointmentId(), payload.durationDays(), payload.issuedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.NOTIFICATION_SENT)
    @IdempotentConsumer
    public void onNotificationSent(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<NotificationSent> envelope = readEnvelope(record.value(), NotificationSent.class);
        NotificationSent payload = envelope.payload();
        kpiRecordingService.recordNotificationSent(
                envelope.eventId(),
                payload.appointmentId() != null ? payload.appointmentId() : payload.notificationId(),
                payload.channel(),
                payload.sentAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.CONNECTION_QUALITY_DEGRADED)
    @IdempotentConsumer
    public void onConnectionQualityDegraded(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<ConnectionQualityDegraded> envelope =
                readEnvelope(record.value(), ConnectionQualityDegraded.class);
        ConnectionQualityDegraded payload = envelope.payload();
        kpiRecordingService.recordConnectionQualityDegraded(
                envelope.eventId(), payload.appointmentId(), payload.qualityScore(), payload.detectedAt());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.INTEGRATION_FAILED)
    @IdempotentConsumer
    public void onIntegrationFailed(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<IntegrationFailed> envelope = readEnvelope(record.value(), IntegrationFailed.class);
        IntegrationFailed payload = envelope.payload();
        kpiRecordingService.recordIntegrationFailed(
                envelope.eventId(), payload.subjectRef(), payload.failedAt());
        acknowledgment.acknowledge();
        log.debug("Recorded integration failure KPI for {}", payload.subjectRef());
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
