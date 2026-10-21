package com.telemedicine.physicianservice.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * The physician "review &amp; join" detail for one appointment (prototype D-03). Every field comes
 * from the projected {@code ConsultationContextPublished} event plus the worklist row's live
 * readiness/status; the physician service adds no clinical data of its own.
 *
 * @param appointmentId appointment reference
 * @param patientDisplay human display label for the patient
 * @param ageYears patient age, 0 when unknown
 * @param sexCode coded sex, or null
 * @param city patient city/location, or null
 * @param insuranceScheme coded insurance scheme/tier, or null
 * @param nationalIdMasked masked national id, or null
 * @param modality VIDEO | AUDIO | CHAT
 * @param visitType FIRST_VIRTUAL | FOLLOW_UP | ...
 * @param scheduledAt agreed start time
 * @param severityScore 0-10 self-reported severity, 0 when not captured
 * @param symptomDurationCode coded duration bucket, or null
 * @param symptomCount number of symptoms captured
 * @param attachmentCount number of attachments
 * @param redFlag whether triage raised a red flag
 * @param consultationFeeMinor gross fee in minor units
 * @param voucherMinor insurance voucher in minor units
 * @param amountPaidMinor amount paid in minor units
 * @param currency ISO currency code
 * @param paymentStatus PAID | UNPAID | ...
 * @param clinicalContext parsed clinical-context document (reasonText, symptoms, quickCheck,
 *     medicalHistory, previousEncounters, attachments), or null when none was published
 * @param readiness worklist readiness: NOT_READY | IN_PROGRESS | READY
 * @param consultationStatus worklist status: SCHEDULED | CHECKED_IN | OPEN_DOCTOR | STARTED | CLOSED
 * @param technicalStatus OK | DEGRADED | IMPAIRED
 * @param enteredAt when the patient entered the waiting room, or null if not arrived
 * @param redFlagAcknowledged whether a present red flag was acknowledged
 * @param contextAvailable whether a consultation-context event has been projected yet
 */
public record PatientDetailResponse(
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
        JsonNode clinicalContext,
        String readiness,
        String consultationStatus,
        String technicalStatus,
        Instant enteredAt,
        boolean redFlagAcknowledged,
        boolean contextAvailable) {}
