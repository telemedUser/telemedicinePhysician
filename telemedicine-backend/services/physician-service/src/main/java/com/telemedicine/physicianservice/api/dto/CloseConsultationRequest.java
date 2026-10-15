package com.telemedicine.physicianservice.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Physician closure submission (PRD §14.2, TM-DOC-008/009).
 *
 * @param completedDocuments document types the physician completed (NOTE, DIAGNOSIS, PRESCRIPTION,
 *     CERTIFICATE, REFERRAL, FOLLOW_UP)
 * @param exceptionReason when set, records an audited exception that permits closure without the
 *     full minimum completion set
 */
public record CloseConsultationRequest(
        @NotNull List<String> completedDocuments, String exceptionReason) {}
