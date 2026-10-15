package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A medical leave certificate was issued.
 *
 * <p>The duration is published because downstream scheduling and reporting need it; the diagnosis
 * that justified it is not.
 *
 * @param appointmentId appointment the certificate belongs to
 * @param medicalLeaveId identifier returned by the provider
 * @param provider issuing provider, for example {@code medipass}
 * @param physicianRef pseudonymous reference to the issuing physician
 * @param durationDays number of days certified
 * @param issuedAt when the certificate was issued
 */
public record MedicalLeaveIssued(
        String appointmentId,
        String medicalLeaveId,
        String provider,
        String physicianRef,
        int durationDays,
        Instant issuedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.MEDICAL_LEAVE_ISSUED;
    }
}
