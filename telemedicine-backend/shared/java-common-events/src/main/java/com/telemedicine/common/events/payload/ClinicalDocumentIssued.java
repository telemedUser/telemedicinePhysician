package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A clinical document was generated and stored.
 *
 * @param appointmentId appointment the document belongs to
 * @param documentId identifier in the document store
 * @param documentType coded type, for example {@code CONSULTATION_SUMMARY}
 * @param physicianRef pseudonymous reference to the issuing physician
 * @param issuedAt when the document was issued
 */
public record ClinicalDocumentIssued(
        String appointmentId,
        String documentId,
        String documentType,
        String physicianRef,
        Instant issuedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CLINICAL_DOCUMENT_ISSUED;
    }
}
