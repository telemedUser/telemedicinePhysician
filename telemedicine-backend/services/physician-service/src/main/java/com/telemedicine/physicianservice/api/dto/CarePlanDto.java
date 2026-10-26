package com.telemedicine.physicianservice.api.dto;

import java.util.List;

/**
 * The full D-08 care plan, used for both GET (load) and PUT (save). The physician app sends the
 * whole plan on each save; edit→done is a UI concern, the backend just persists current state.
 */
public record CarePlanDto(
        String appointmentId,
        ClinicalNote note,
        List<Diagnosis> diagnoses,
        List<Prescription> prescriptions,
        List<DiagnosticTest> diagnosticTests,
        FollowUp followUp,
        MedicalLeave medicalLeave,
        Summary summary,
        String status) {

    /** Four-section clinical note. */
    public record ClinicalNote(String history, String findings, String assessment, String plan) {}

    /** A selected diagnosis (ICD-10). */
    public record Diagnosis(String code, String title, boolean primary) {}

    /** A prescribed medication line. */
    public record Prescription(
            String medicationId,
            String name,
            String strength,
            String form,
            String dosage,
            String frequency,
            Integer durationDays,
            Integer quantity,
            String instructions) {}

    /** An ordered diagnostic test. */
    public record DiagnosticTest(
            String testId, String name, String category, String specimen, String note) {}

    /** Follow-up appointment intent. */
    public record FollowUp(
            boolean enabled, String mode, Integer inDays, String date, String instructions) {}

    /** Medical leave / sick note. */
    public record MedicalLeave(
            boolean enabled, String reason, String startDate, String endDate) {}

    /** Patient-facing plain-language summary. */
    public record Summary(String text, String instructions) {}
}
