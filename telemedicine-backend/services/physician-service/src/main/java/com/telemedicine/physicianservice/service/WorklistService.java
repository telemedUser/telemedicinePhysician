package com.telemedicine.physicianservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.physicianservice.api.dto.WorklistEntryResponse;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemEntity;
import com.telemedicine.physicianservice.domain.worklist.WorklistItemRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves and maintains the physician worklist read model (PRD §12.3). Reads are market-scoped
 * (TM-MKT-002); the projection itself is maintained by the Kafka listeners.
 */
@Service
public class WorklistService {

    private static final Logger log = LoggerFactory.getLogger(WorklistService.class);
    private static final TypeReference<List<String>> FLAG_LIST = new TypeReference<>() {};

    private final WorklistItemRepository worklistItemRepository;
    private final ObjectMapper objectMapper;

    public WorklistService(
            WorklistItemRepository worklistItemRepository, ObjectMapper objectMapper) {
        this.worklistItemRepository = worklistItemRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<WorklistEntryResponse> worklistFor(String providerId, String market) {
        return worklistItemRepository
                .findByProviderIdAndMarketOrderByEnteredAtAsc(providerId, market)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Records a physician's acknowledgement of a red flag before joining (TM-WLT-009). */
    @Transactional
    public void acknowledgeRedFlag(String appointmentId, String physicianRef) {
        worklistItemRepository
                .findById(appointmentId)
                .ifPresent(
                        item -> {
                            item.setRedFlagAcknowledgedAt(Instant.now());
                            item.setRedFlagAcknowledgedBy(physicianRef);
                            item.setUpdatedAt(Instant.now());
                            worklistItemRepository.save(item);
                            log.info("Red flag acknowledged for {}", appointmentId);
                        });
    }

    /**
     * Admit the patient into the consultation (PRD §12, "admit" → OPEN_DOCTOR). In the full
     * architecture this is owned by waiting-room-service and propagated via a StatusChanged event;
     * for the direct-to-service review flow this transitions the worklist read model so the UI sees
     * the state change immediately.
     */
    @Transactional
    public WorklistEntryResponse admit(String appointmentId) {
        WorklistItemEntity item =
                worklistItemRepository
                        .findById(appointmentId)
                        .orElseThrow(
                                () ->
                                        com.telemedicine.common.core.error.NotFoundException.of(
                                                "worklistItem", appointmentId));
        item.setConsultationStatus("OPEN_DOCTOR");
        item.setUpdatedAt(Instant.now());
        worklistItemRepository.save(item);
        log.info("Admitted patient for appointment {} (OPEN_DOCTOR)", appointmentId);
        return toResponse(item);
    }

    private WorklistEntryResponse toResponse(WorklistItemEntity item) {
        List<String> flags = parseFlags(item.getFlags());
        long waitingSeconds =
                item.getEnteredAt() == null
                        ? 0
                        : Math.max(0, Instant.now().getEpochSecond() - item.getEnteredAt().getEpochSecond());
        return new WorklistEntryResponse(
                item.getAppointmentId(),
                item.getPatientRef(),
                item.getMode(),
                item.getEnteredAt(),
                waitingSeconds,
                item.getReadiness(),
                item.getConsultationStatus(),
                item.getTechnicalStatus(),
                item.isTelemetryAvailable(),
                flags,
                item.getRedFlagAcknowledgedAt() != null);
    }

    private List<String> parseFlags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, FLAG_LIST);
        } catch (Exception exception) {
            log.debug("Could not parse worklist flags: {}", exception.getMessage());
            return List.of();
        }
    }
}
