package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A patient accepted a consent, adding an entry to the immutable consent ledger.
 *
 * @param consentId ledger entry identifier
 * @param subjectRef pseudonymous reference to the consenting patient
 * @param consentType for example {@code PLATFORM_TERMS} or {@code CLINICAL_INFORMED}
 * @param version version of the consent text accepted
 * @param grantedAt when acceptance was recorded
 */
public record ConsentGranted(
        String consentId, String subjectRef, String consentType, String version, Instant grantedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CONSENT_GRANTED;
    }
}
