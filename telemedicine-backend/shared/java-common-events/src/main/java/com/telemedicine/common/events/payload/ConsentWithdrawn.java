package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A patient withdrew a consent previously given.
 *
 * @param consentId ledger entry identifier of the withdrawal
 * @param subjectRef pseudonymous reference to the patient
 * @param consentType consent being withdrawn
 * @param withdrawnAt when the withdrawal was recorded
 */
public record ConsentWithdrawn(
        String consentId, String subjectRef, String consentType, Instant withdrawnAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CONSENT_WITHDRAWN;
    }
}
