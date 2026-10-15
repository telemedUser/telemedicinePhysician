package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * An electronic prescription was issued through the prescription provider.
 *
 * <p>Medication, dose and diagnosis are clinical data and are not published.
 *
 * @param appointmentId appointment the prescription belongs to
 * @param prescriptionId identifier returned by the provider
 * @param provider issuing provider, for example {@code recmed}
 * @param physicianRef pseudonymous reference to the prescribing physician
 * @param issuedAt when the prescription was issued
 */
public record PrescriptionIssued(
        String appointmentId,
        String prescriptionId,
        String provider,
        String physicianRef,
        Instant issuedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.PRESCRIPTION_ISSUED;
    }
}
