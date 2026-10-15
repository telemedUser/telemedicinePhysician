package com.telemedicine.notificationservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.AppointmentRescheduled;
import com.telemedicine.common.events.payload.ClinicalDocumentIssued;
import com.telemedicine.common.events.payload.IdentityValidationFailed;
import com.telemedicine.common.events.payload.LinkGenerated;
import com.telemedicine.common.events.payload.MedicalLeaveIssued;
import com.telemedicine.common.events.payload.PrescriptionIssued;
import com.telemedicine.common.events.payload.StatusChanged;
import com.telemedicine.common.events.payload.SurveyRequested;
import com.telemedicine.notificationservice.service.NotificationService;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class NotificationDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDomainEventListener.class);

    private final ObjectMapper eventObjectMapper;
    private final NotificationService notificationService;

    public NotificationDomainEventListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            NotificationService notificationService) {
        this.eventObjectMapper = eventObjectMapper;
        this.notificationService = notificationService;
    }

    @DomainEventListener(EventTypes.LINK_GENERATED)
    @IdempotentConsumer
    public void onLinkGenerated(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<LinkGenerated> envelope = readEnvelope(record.value(), LinkGenerated.class);
        LinkGenerated payload = envelope.payload();
        notificationService.dispatchEvent(
                "session-link",
                payload.channel(),
                payload.appointmentId(),
                payload.appointmentId(),
                Map.of("appointmentId", payload.appointmentId()));
        acknowledgment.acknowledge();
        log.debug("Handled LinkGenerated for {}", payload.appointmentId());
    }

    @DomainEventListener(EventTypes.STATUS_CHANGED)
    @IdempotentConsumer
    public void onStatusChanged(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<StatusChanged> envelope = readEnvelope(record.value(), StatusChanged.class);
        StatusChanged payload = envelope.payload();
        if (!"OPEN_DOCTOR".equalsIgnoreCase(payload.toStatus())) {
            acknowledgment.acknowledge();
            return;
        }
        notificationService.dispatchEvent(
                "physician-ready",
                "sms",
                payload.appointmentId(),
                payload.appointmentId(),
                Map.of("appointmentId", payload.appointmentId()));
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.CLINICAL_DOCUMENT_ISSUED)
    @IdempotentConsumer
    public void onClinicalDocumentIssued(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<ClinicalDocumentIssued> envelope =
                readEnvelope(record.value(), ClinicalDocumentIssued.class);
        ClinicalDocumentIssued payload = envelope.payload();
        notificationService.dispatchEvent(
                "clinical-document-ready",
                "email",
                payload.appointmentId(),
                payload.appointmentId(),
                Map.of(
                        "appointmentId", payload.appointmentId(),
                        "documentType", payload.documentType()));
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.SURVEY_REQUESTED)
    @IdempotentConsumer
    public void onSurveyRequested(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<SurveyRequested> envelope = readEnvelope(record.value(), SurveyRequested.class);
        SurveyRequested payload = envelope.payload();
        notificationService.dispatchEvent(
                "survey-request",
                payload.channel(),
                payload.appointmentId(),
                payload.appointmentId(),
                Map.of("appointmentId", payload.appointmentId()));
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.APPOINTMENT_RESCHEDULED)
    @IdempotentConsumer
    public void onAppointmentRescheduled(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<AppointmentRescheduled> envelope =
                readEnvelope(record.value(), AppointmentRescheduled.class);
        AppointmentRescheduled payload = envelope.payload();
        notificationService.dispatchEvent(
                "appointment-rescheduled",
                "email",
                payload.appointmentId(),
                payload.appointmentId(),
                Map.of("appointmentId", payload.appointmentId()));
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.IDENTITY_VALIDATION_FAILED)
    @IdempotentConsumer
    public void onIdentityValidationFailed(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<IdentityValidationFailed> envelope =
                readEnvelope(record.value(), IdentityValidationFailed.class);
        IdentityValidationFailed payload = envelope.payload();
        if (payload.attempt() >= 3) {
            notificationService.dispatchEvent(
                    "identity-lockout",
                    "email",
                    payload.appointmentId(),
                    payload.appointmentId(),
                    Map.of("appointmentId", payload.appointmentId()));
        }
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.PRESCRIPTION_ISSUED)
    @IdempotentConsumer
    public void onPrescriptionIssued(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<PrescriptionIssued> envelope = readEnvelope(record.value(), PrescriptionIssued.class);
        PrescriptionIssued payload = envelope.payload();
        notificationService.dispatchEvent(
                "prescription-ready",
                "email",
                payload.appointmentId(),
                payload.appointmentId(),
                Map.of("appointmentId", payload.appointmentId()));
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.MEDICAL_LEAVE_ISSUED)
    @IdempotentConsumer
    public void onMedicalLeaveIssued(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<MedicalLeaveIssued> envelope = readEnvelope(record.value(), MedicalLeaveIssued.class);
        MedicalLeaveIssued payload = envelope.payload();
        notificationService.dispatchEvent(
                "medical-leave-ready",
                "email",
                payload.appointmentId(),
                payload.appointmentId(),
                Map.of("appointmentId", payload.appointmentId()));
        acknowledgment.acknowledge();
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
