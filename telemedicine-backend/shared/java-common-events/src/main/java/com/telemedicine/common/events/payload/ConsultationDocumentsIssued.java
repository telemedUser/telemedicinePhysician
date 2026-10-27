package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The set of clinical documents a physician issued when closing a consultation (prototype D-10),
 * published by physician-service so the patient domain can project a "your documents are ready"
 * read model for the patient's post-consultation screen (P-19) without a synchronous call back into
 * the physician domain.
 *
 * <p>Like {@link ConsultationContextPublished}, this payload keeps the catalogue body flat and
 * scalar-only: identifiers, counts, boolean presence flags, a coded status, a timestamp and a
 * single opaque {@code documentsJson} string. The string carries the richer document detail
 * (prescription lines, medical-leave dates, summary text, diagnosis codes, follow-up) as a compact
 * JSON document — a scalar on the wire, parsed by the consumer. No field here is a patient
 * identifier in the clear, keeping it inside the platform's PHI-free event rule and the schema
 * contract.
 *
 * @param appointmentId appointment the documents belong to (also the Kafka partition key)
 * @param status coded issue status, for example {@code ISSUED}
 * @param documentCount number of documents issued
 * @param hasPrescription whether a prescription was issued
 * @param hasMedicalLeave whether a medical-leave certificate was issued
 * @param hasSummary whether a patient summary was issued
 * @param prescriptionNumber issued prescription document number, or null when none
 * @param medicalLeaveNumber issued medical-leave document number, or null when none
 * @param documentsJson compact JSON document carrying the variable-length document detail
 * @param issuedAt when the documents were issued (consultation close time)
 */
public record ConsultationDocumentsIssued(
        String appointmentId,
        String status,
        int documentCount,
        boolean hasPrescription,
        boolean hasMedicalLeave,
        boolean hasSummary,
        String prescriptionNumber,
        String medicalLeaveNumber,
        String documentsJson,
        Instant issuedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CONSULTATION_DOCUMENTS_ISSUED;
    }
}
