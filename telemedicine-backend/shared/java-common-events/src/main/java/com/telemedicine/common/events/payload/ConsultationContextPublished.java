package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The booking/clinical context a physician needs to review a patient before joining (prototype
 * D-03). Published by patient-booking-service once the patient has submitted their booking and
 * pre-join details, so the physician service can project a read model for the detail view without a
 * synchronous call back into the patient domain.
 *
 * <p>This payload deliberately carries only coded values, counts, numeric scores and a single
 * opaque {@code clinicalContextJson} string. The string holds the richer, variable-length context
 * (symptoms, quick-check answers, medical-history lines, previous encounters, attachment metadata)
 * as a compact JSON document — a scalar on the wire, parsed by the consumer. No field here is a
 * patient identifier in the clear: {@code patientDisplay} is the same human label already carried
 * as {@code patientRef} elsewhere (not a national id), and the JSON is booking-supplied context,
 * not demographics keyed to an identity. Keeping the catalogue payload flat and scalar-only keeps
 * it inside the platform's PHI-free event rule and the schema contract.
 *
 * @param appointmentId appointment the context belongs to
 * @param patientDisplay human display label for the patient (same value as the worklist patientRef)
 * @param ageYears patient age in years, or 0 when unknown
 * @param sexCode coded sex (for example {@code F}, {@code M}, {@code X}), or null when unknown
 * @param city patient city/location label, or null when unknown
 * @param insuranceScheme coded insurance scheme/tier label, or null when unknown
 * @param nationalIdMasked masked national identifier (never the full value), or null
 * @param modality consultation modality, for example {@code VIDEO} or {@code AUDIO}
 * @param visitType coded visit type, for example {@code FIRST_VIRTUAL} or {@code FOLLOW_UP}
 * @param scheduledAt agreed start time
 * @param severityScore self-reported severity on a 0-10 scale, or 0 when not captured
 * @param symptomDurationCode coded duration bucket, for example {@code DAYS_2}, or null
 * @param symptomCount number of symptoms captured
 * @param attachmentCount number of attached media items
 * @param redFlag whether the triage screen raised a red flag
 * @param consultationFeeMinor gross consultation fee in minor units, or 0 when unknown
 * @param voucherMinor insurance voucher/discount in minor units, or 0 when none
 * @param amountPaidMinor amount paid in minor units, or 0 when unpaid
 * @param currency ISO currency code, for example {@code INR}
 * @param paymentStatus coded payment status, for example {@code PAID} or {@code UNPAID}
 * @param clinicalContextJson compact JSON document carrying the variable-length detail
 * @param publishedAt when the context snapshot was published
 */
public record ConsultationContextPublished(
        String appointmentId,
        String patientDisplay,
        int ageYears,
        String sexCode,
        String city,
        String insuranceScheme,
        String nationalIdMasked,
        String modality,
        String visitType,
        Instant scheduledAt,
        int severityScore,
        String symptomDurationCode,
        int symptomCount,
        int attachmentCount,
        boolean redFlag,
        long consultationFeeMinor,
        long voucherMinor,
        long amountPaidMinor,
        String currency,
        String paymentStatus,
        String clinicalContextJson,
        Instant publishedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CONSULTATION_CONTEXT_PUBLISHED;
    }
}
