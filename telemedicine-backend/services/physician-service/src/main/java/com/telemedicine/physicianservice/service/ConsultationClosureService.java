package com.telemedicine.physicianservice.service;

import com.telemedicine.common.events.EventPublisher;
import com.telemedicine.common.events.payload.ConsultationClosed;
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

    public ConsultationClosureService(
            ConsultationClosureRepository closureRepository,
            PhysicianServiceProperties properties,
            EventPublisher eventPublisher) {
        this.closureRepository = closureRepository;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
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
        log.info(
                "Consultation {} closed (minimumSatisfied={}, exception={})",
                appointmentId,
                minimumSatisfied,
                exceptionRecorded);

        return new CloseConsultationResponse(appointmentId, true, minimumSatisfied, List.of());
    }
}
