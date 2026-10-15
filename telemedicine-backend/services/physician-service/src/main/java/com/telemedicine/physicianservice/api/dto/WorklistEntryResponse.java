package com.telemedicine.physicianservice.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * A single worklist row as the physician sees it (PRD §12.3, TM-WLT-005).
 *
 * @param appointmentId appointment reference
 * @param patientRef pseudonymous patient reference
 * @param mode consultation mode (video/audio/chat)
 * @param enteredAt when the patient entered the waiting room
 * @param waitingSeconds elapsed waiting time
 * @param readiness NOT_READY | IN_PROGRESS | READY
 * @param consultationStatus CHECKED_IN | OPEN_DOCTOR | STARTED | CLOSED
 * @param technicalStatus OK | DEGRADED | IMPAIRED
 * @param telemetryAvailable whether telemetry data is available for this patient
 * @param flags FIRST_VISIT | FOLLOW_UP | GUEST | RED_FLAG
 * @param redFlagAcknowledged whether a present red flag has been acknowledged (TM-WLT-009)
 */
public record WorklistEntryResponse(
        String appointmentId,
        String patientRef,
        String mode,
        Instant enteredAt,
        long waitingSeconds,
        String readiness,
        String consultationStatus,
        String technicalStatus,
        boolean telemetryAvailable,
        List<String> flags,
        boolean redFlagAcknowledged) {}
