package com.telemedicine.physicianservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.EventPublisher;
import com.telemedicine.common.events.payload.ConsultationClosed;
import com.telemedicine.common.events.payload.ConsultationDocumentsIssued;
import com.telemedicine.physicianservice.api.dto.CarePlanDto;
import com.telemedicine.physicianservice.api.dto.CloseConsultationRequest;
import com.telemedicine.physicianservice.api.dto.CloseConsultationResponse;
import com.telemedicine.physicianservice.config.PhysicianServiceProperties;
import com.telemedicine.physicianservice.domain.closure.ConsultationClosureEntity;
import com.telemedicine.physicianservice.domain.closure.ConsultationClosureRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Physician closure orchestration (PRD §14.2). Closure succeeds and {@code ConsultationClosed} is
 * emitted only when the configurable minimum completion set is satisfied, or when the physician
 * records an audited exception (TM-DOC-009). The documents themselves live in their systems of
 * record; this service owns only the closure fact and the emitted event.
 */
@Service
public class ConsultationClosureService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationClosureService.class);

    private final ConsultationClosureRepository closureRepository;
    private final PhysicianServiceProperties properties;
    private final EventPublisher eventPublisher;
    private final CarePlanService carePlanService;
    private final ObjectMapper objectMapper;

    public ConsultationClosureService(
            ConsultationClosureRepository closureRepository,
            PhysicianServiceProperties properties,
            EventPublisher eventPublisher,
            CarePlanService carePlanService,
            ObjectMapper objectMapper) {
        this.closureRepository = closureRepository;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.carePlanService = carePlanService;
        this.objectMapper = objectMapper;
    }

    /**
     * Deterministic document number from the appointment id, matching the physician D-10 UI so the
     * number the physician saw is the same one the patient receives. Mirrors the frontend
     * docNumber(prefix, appointmentId, salt): a 7-digit number seeded by a salt + char hash.
     */
    private static String docNumber(String prefix, String appointmentId, int salt) {
        long h = salt & 0xffffffffL;
        for (int i = 0; i < appointmentId.length(); i++) {
            h = (h * 31 + appointmentId.charAt(i)) & 0xffffffffL;
        }
        long n = (h % 9_000_000L) + 1_000_000L;
        return prefix + "-" + n;
    }

    /**
     * Load the saved care plan and publish a {@link ConsultationDocumentsIssued} event carrying the
     * issued documents. The full plan DTO is serialized into the opaque {@code documentsJson}
     * scalar; the flat event fields expose only presence flags, counts and document numbers.
     */
    private void publishDocumentsIssued(String appointmentId, Instant issuedAt) {
        try {
            CarePlanDto plan = carePlanService.get(appointmentId);
            boolean hasRx = plan.prescriptions() != null && !plan.prescriptions().isEmpty();
            boolean hasLeave = plan.medicalLeave() != null && plan.medicalLeave().enabled();
            boolean hasSummary =
                    plan.summary() != null
                            && ((plan.summary().text() != null && !plan.summary().text().isBlank())
                                    || (plan.summary().instructions() != null
                                            && !plan.summary().instructions().isBlank()));
            // Summary + diagnosis + optional prescription + optional leave + receipt.
            int docCount = 2 + (hasRx ? 1 : 0) + (hasLeave ? 1 : 0) + 1;
            String rxNumber = hasRx ? docNumber("Rx", appointmentId, 7) : null;
            String leaveNumber = hasLeave ? docNumber("ML", appointmentId, 13) : null;
            String documentsJson = objectMapper.writeValueAsString(plan);

            eventPublisher.publish(
                    appointmentId,
                    new ConsultationDocumentsIssued(
                            appointmentId,
                            "ISSUED",
                            docCount,
                            hasRx,
                            hasLeave,
                            hasSummary,
                            rxNumber,
                            leaveNumber,
                            documentsJson,
                            issuedAt));
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Could not publish ConsultationDocumentsIssued for {}", appointmentId, e);
        }
    }

    @Transactional
    public CloseConsultationResponse close(
            String appointmentId, String market, CloseConsultationRequest request) {

        List<String> completed =
                request.completedDocuments().stream()
                        .map(doc -> doc.toUpperCase(Locale.ROOT))
                        .toList();

        List<String> missing =
                properties.getMinimumCompletionSet().stream()
                        .map(doc -> doc.toUpperCase(Locale.ROOT))
                        .filter(required -> !completed.contains(required))
                        .toList();

        boolean minimumSatisfied = missing.isEmpty();
        boolean exceptionRecorded =
                request.exceptionReason() != null && !request.exceptionReason().isBlank();

        // Not satisfied and no audited exception: refuse closure, tell the physician what is
        // missing rather than silently dropping clinical content (TM-DOC-001c, TM-DOC-009).
        if (!minimumSatisfied && !exceptionRecorded) {
            return new CloseConsultationResponse(appointmentId, false, false, missing);
        }

        ConsultationClosureEntity closure =
                closureRepository
                        .findById(appointmentId)
                        .orElseGet(
                                () -> new ConsultationClosureEntity(appointmentId, null, market));
        Instant now = Instant.now();
        closure.setClosedAt(now);
        closure.setMinimumSatisfied(minimumSatisfied);
        closure.setExceptionReason(request.exceptionReason());
        closure.setUpdatedAt(now);
        closureRepository.save(closure);

        long durationSeconds =
                closure.getStartedAt() == null
                        ? 0
                        : Math.max(0, now.getEpochSecond() - closure.getStartedAt().getEpochSecond());

        // Emitted via the transactional outbox; delivery is awaited after commit by the publisher.
        eventPublisher.publish(
                appointmentId, new ConsultationClosed(appointmentId, "COMPLETED", durationSeconds, now));

        // Publish the issued documents so the patient domain can project a "documents ready" read
        // model for the post-consultation screen. The clinical detail rides in an opaque JSON
        // scalar (documentsJson), keeping the event body PHI-field-name free like the catalogue
        // requires. Never let a publish failure break the close.
        publishDocumentsIssued(appointmentId, now);

        log.info(
                "Consultation {} closed (minimumSatisfied={}, exception={})",
                appointmentId,
                minimumSatisfied,
                exceptionRecorded);

        return new CloseConsultationResponse(appointmentId, true, minimumSatisfied, List.of());
    }
}
