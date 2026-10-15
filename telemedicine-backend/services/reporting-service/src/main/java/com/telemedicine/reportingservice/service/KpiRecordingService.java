package com.telemedicine.reportingservice.service;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.reportingservice.domain.JourneyMilestoneEntity;
import com.telemedicine.reportingservice.domain.JourneyMilestoneRepository;
import com.telemedicine.reportingservice.domain.KpiEventEntity;
import com.telemedicine.reportingservice.domain.KpiEventRepository;
import com.telemedicine.reportingservice.domain.KpiFamily;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KpiRecordingService {

    public static final String METRIC_PAYMENT_SUCCESS = "telemedicine.journey.payment.success";
    public static final String METRIC_PAYMENT_ATTEMPT = "telemedicine.journey.payment.attempt";
    public static final String METRIC_PATIENT_NPS = "telemedicine.journey.patient.nps";
    public static final String METRIC_INTEGRATION_INCIDENT = "telemedicine.technical.integration.incident";
    public static final String METRIC_AVAILABILITY = "telemedicine.technical.availability";
    public static final String METRIC_NOTIFICATION_SENT = "telemedicine.functional.notification.sent";
    public static final String METRIC_PRESCRIPTION_ISSUED = "telemedicine.functional.prescription.issued";
    public static final String METRIC_MEDICAL_LEAVE_ISSUED = "telemedicine.functional.medical_leave.issued";
    public static final String METRIC_CONNECTION_QUALITY = "telemedicine.technical.connection.quality";

    private static final String MILESTONE_WAITING_ROOM = "WAITING_ROOM";
    private static final String MILESTONE_VIDEO_CREATED = "VIDEO_CREATED";
    private static final String MILESTONE_CONSULTATION_CLOSED = "CONSULTATION_CLOSED";

    private final KpiEventRepository kpiEventRepository;
    private final JourneyMilestoneRepository milestoneRepository;

    public KpiRecordingService(
            KpiEventRepository kpiEventRepository, JourneyMilestoneRepository milestoneRepository) {
        this.kpiEventRepository = kpiEventRepository;
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional
    public void recordWaitingRoomEntry(String sourceEventId, String appointmentId, Instant occurredAt) {
        saveMilestone(appointmentId, MILESTONE_WAITING_ROOM, occurredAt);
    }

    @Transactional
    public void recordOpenDoctor(String sourceEventId, String appointmentId, Instant occurredAt) {
        milestoneRepository
                .findByAppointmentIdAndMilestone(appointmentId, MILESTONE_WAITING_ROOM)
                .ifPresent(
                        waiting ->
                                recordDurationMetric(
                                        sourceEventId,
                                        appointmentId,
                                        JourneyMetrics.METRIC_WAITING_TIME,
                                        KpiFamily.FUNCTIONAL,
                                        waiting.getOccurredAt(),
                                        occurredAt,
                                        "StatusChanged"));
    }

    @Transactional
    public void recordVideoSessionCreated(String sourceEventId, String appointmentId, Instant occurredAt) {
        saveMilestone(appointmentId, MILESTONE_VIDEO_CREATED, occurredAt);
    }

    @Transactional
    public void recordVideoSessionStarted(String sourceEventId, String appointmentId, Instant occurredAt) {
        milestoneRepository
                .findByAppointmentIdAndMilestone(appointmentId, MILESTONE_VIDEO_CREATED)
                .ifPresent(
                        created ->
                                recordDurationMetric(
                                        sourceEventId,
                                        appointmentId,
                                        JourneyMetrics.METRIC_VIDEO_ENTER_TIME,
                                        KpiFamily.FUNCTIONAL,
                                        created.getOccurredAt(),
                                        occurredAt,
                                        "VideoSessionStarted"));
    }

    @Transactional
    public void recordPaymentConfirmed(String sourceEventId, String appointmentId, Instant occurredAt) {
        recordCounter(sourceEventId, appointmentId, METRIC_PAYMENT_SUCCESS, KpiFamily.FUNCTIONAL, 1.0, occurredAt, "PaymentConfirmed");
        recordCounter(sourceEventId + ":attempt", appointmentId, METRIC_PAYMENT_ATTEMPT, KpiFamily.FUNCTIONAL, 1.0, occurredAt, "PaymentConfirmed");
    }

    @Transactional
    public void recordConsultationClosed(
            String sourceEventId, String appointmentId, String outcome, Instant occurredAt) {
        saveMilestone(appointmentId, MILESTONE_CONSULTATION_CLOSED, occurredAt);
        if ("COMPLETED".equalsIgnoreCase(outcome)) {
            recordCounter(
                    sourceEventId,
                    appointmentId,
                    JourneyMetrics.METRIC_CONSULTATION_COMPLETED,
                    KpiFamily.FUNCTIONAL,
                    1.0,
                    occurredAt,
                    "ConsultationClosed");
        } else {
            recordCounter(
                    sourceEventId,
                    appointmentId,
                    JourneyMetrics.METRIC_CONSULTATION_ABANDONED,
                    KpiFamily.FUNCTIONAL,
                    1.0,
                    occurredAt,
                    "ConsultationClosed");
        }
    }

    @Transactional
    public void recordDocumentIssued(String sourceEventId, String appointmentId, Instant issuedAt) {
        milestoneRepository
                .findByAppointmentIdAndMilestone(appointmentId, MILESTONE_CONSULTATION_CLOSED)
                .ifPresent(
                        closed ->
                                recordDurationMetric(
                                        sourceEventId,
                                        appointmentId,
                                        JourneyMetrics.METRIC_DOCUMENT_ISSUANCE_TIME,
                                        KpiFamily.FUNCTIONAL,
                                        closed.getOccurredAt(),
                                        issuedAt,
                                        "ClinicalDocumentIssued"));
    }

    @Transactional
    public void recordSurveyCompleted(String sourceEventId, String appointmentId, int score, Instant occurredAt) {
        recordCounter(
                sourceEventId,
                appointmentId,
                METRIC_PATIENT_NPS,
                KpiFamily.BUSINESS,
                score,
                occurredAt,
                "SurveyCompleted");
    }

    @Transactional
    public void recordIntegrationFailed(String sourceEventId, String subjectRef, Instant occurredAt) {
        recordCounter(
                sourceEventId,
                subjectRef,
                METRIC_INTEGRATION_INCIDENT,
                KpiFamily.TECHNICAL,
                1.0,
                occurredAt,
                "IntegrationFailed");
    }

    @Transactional
    public void recordPrescriptionIssued(String sourceEventId, String appointmentId, Instant issuedAt) {
        recordCounter(
                sourceEventId,
                appointmentId,
                METRIC_PRESCRIPTION_ISSUED,
                KpiFamily.FUNCTIONAL,
                1.0,
                issuedAt,
                "PrescriptionIssued");
    }

    @Transactional
    public void recordMedicalLeaveIssued(
            String sourceEventId, String appointmentId, int durationDays, Instant issuedAt) {
        recordCounter(
                sourceEventId,
                appointmentId,
                METRIC_MEDICAL_LEAVE_ISSUED,
                KpiFamily.FUNCTIONAL,
                durationDays,
                issuedAt,
                "MedicalLeaveIssued");
    }

    @Transactional
    public void recordNotificationSent(
            String sourceEventId, String appointmentId, String channel, Instant sentAt) {
        recordCounter(
                sourceEventId,
                appointmentId,
                METRIC_NOTIFICATION_SENT + "." + channel.toLowerCase(Locale.ROOT),
                KpiFamily.FUNCTIONAL,
                1.0,
                sentAt,
                "NotificationSent");
    }

    @Transactional
    public void recordConnectionQualityDegraded(
            String sourceEventId, String appointmentId, int qualityScore, Instant detectedAt) {
        recordCounter(
                sourceEventId,
                appointmentId,
                METRIC_CONNECTION_QUALITY,
                KpiFamily.TECHNICAL,
                qualityScore,
                detectedAt,
                "ConnectionQualityDegraded");
    }

    private void recordDurationMetric(
            String sourceEventId,
            String appointmentId,
            String metricName,
            KpiFamily family,
            Instant start,
            Instant end,
            String eventType) {
        double seconds = Duration.between(start, end).toMillis() / 1000.0;
        if (seconds < 0) {
            return;
        }
        persistEvent(
                sourceEventId,
                eventType,
                appointmentId,
                family,
                metricName,
                seconds,
                end);
    }

    private void recordCounter(
            String sourceEventId,
            String appointmentId,
            String metricName,
            KpiFamily family,
            double value,
            Instant occurredAt,
            String eventType) {
        persistEvent(sourceEventId, eventType, appointmentId, family, metricName, value, occurredAt);
    }

    private void persistEvent(
            String sourceEventId,
            String eventType,
            String appointmentId,
            KpiFamily family,
            String metricName,
            double value,
            Instant recordedAt) {
        KpiEventEntity entity = new KpiEventEntity();
        entity.setId(IdGenerator.newUlid());
        entity.setSourceEventId(sourceEventId);
        entity.setEventType(eventType);
        entity.setAppointmentId(appointmentId);
        entity.setMetricFamily(family.name());
        entity.setMetricName(metricName);
        entity.setNumericValue(value);
        entity.setRecordedAt(recordedAt);
        entity.setCreatedAt(Instant.now());
        try {
            kpiEventRepository.save(entity);
        } catch (DataIntegrityViolationException ignored) {
            // Idempotent on (source_event_id, metric_name)
        }
    }

    private void saveMilestone(String appointmentId, String milestone, Instant occurredAt) {
        Optional<JourneyMilestoneEntity> existing =
                milestoneRepository.findByAppointmentIdAndMilestone(appointmentId, milestone);
        if (existing.isPresent()) {
            return;
        }
        JourneyMilestoneEntity entity = new JourneyMilestoneEntity();
        entity.setAppointmentId(appointmentId);
        entity.setMilestone(milestone);
        entity.setOccurredAt(occurredAt);
        milestoneRepository.save(entity);
    }
}
