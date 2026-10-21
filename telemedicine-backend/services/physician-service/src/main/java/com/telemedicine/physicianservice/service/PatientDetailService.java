package com.telemedicine.physicianservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.physicianservice.api.dto.PatientDetailResponse;
import com.telemedicine.physicianservice.domain.detail.PatientDetailEntity;
import com.telemedicine.physicianservice.domain.detail.PatientDetailRepository;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemEntity;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves the physician "review &amp; join" detail (prototype D-03) for one appointment by combining
 * the {@code patient_detail} projection (booking/clinical context from {@code
 * ConsultationContextPublished}) with the live worklist row (readiness/status/arrival). Access is
 * scoped to the signed-in physician: a doctor can only open the detail for an appointment on their
 * own worklist.
 */
@Service
public class PatientDetailService {

    private static final Logger log = LoggerFactory.getLogger(PatientDetailService.class);

    private final PatientDetailRepository patientDetailRepository;
    private final WorklistItemRepository worklistItemRepository;
    private final ObjectMapper objectMapper;

    public PatientDetailService(
            PatientDetailRepository patientDetailRepository,
            WorklistItemRepository worklistItemRepository,
            ObjectMapper objectMapper) {
        this.patientDetailRepository = patientDetailRepository;
        this.worklistItemRepository = worklistItemRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PatientDetailResponse detailFor(String physicianId, String appointmentId) {
        WorklistItemEntity worklist =
                worklistItemRepository.findById(appointmentId).orElse(null);

        // Scope: the appointment must be on the signed-in physician's worklist.
        if (worklist == null || !physicianId.equals(worklist.getProviderId())) {
            throw NotFoundException.of("patientDetail", appointmentId);
        }

        PatientDetailEntity detail = patientDetailRepository.findById(appointmentId).orElse(null);
        boolean contextAvailable = detail != null;

        return new PatientDetailResponse(
                appointmentId,
                displayOf(detail, worklist),
                detail != null ? detail.getAgeYears() : 0,
                detail != null ? detail.getSexCode() : null,
                detail != null ? detail.getCity() : null,
                detail != null ? detail.getInsuranceScheme() : null,
                detail != null ? detail.getNationalIdMasked() : null,
                modalityOf(detail, worklist),
                detail != null ? detail.getVisitType() : null,
                detail != null ? detail.getScheduledAt() : null,
                detail != null ? detail.getSeverityScore() : 0,
                detail != null ? detail.getSymptomDurationCode() : null,
                detail != null ? detail.getSymptomCount() : 0,
                detail != null ? detail.getAttachmentCount() : 0,
                detail != null ? detail.isRedFlag() : worklistHasRedFlag(worklist),
                detail != null ? detail.getConsultationFeeMinor() : 0,
                detail != null ? detail.getVoucherMinor() : 0,
                detail != null ? detail.getAmountPaidMinor() : 0,
                detail != null ? detail.getCurrency() : "INR",
                detail != null ? detail.getPaymentStatus() : null,
                parseContext(detail),
                worklist.getReadiness(),
                worklist.getConsultationStatus(),
                worklist.getTechnicalStatus(),
                worklist.getEnteredAt(),
                worklist.getRedFlagAcknowledgedAt() != null,
                contextAvailable);
    }

    private static String displayOf(PatientDetailEntity detail, WorklistItemEntity worklist) {
        if (detail != null && detail.getPatientDisplay() != null && !detail.getPatientDisplay().isBlank()) {
            return detail.getPatientDisplay();
        }
        return worklist.getPatientRef();
    }

    private static String modalityOf(PatientDetailEntity detail, WorklistItemEntity worklist) {
        if (detail != null && detail.getModality() != null) {
            return detail.getModality();
        }
        return worklist.getMode() != null ? worklist.getMode().toUpperCase(java.util.Locale.ROOT) : null;
    }

    private static boolean worklistHasRedFlag(WorklistItemEntity worklist) {
        String flags = worklist.getFlags();
        return flags != null && flags.contains("RED_FLAG");
    }

    private JsonNode parseContext(PatientDetailEntity detail) {
        if (detail == null || detail.getContextJson() == null || detail.getContextJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(detail.getContextJson());
        } catch (Exception e) {
            log.debug("Could not parse clinical context for {}: {}", detail.getAppointmentId(), e.getMessage());
            return null;
        }
    }
}
