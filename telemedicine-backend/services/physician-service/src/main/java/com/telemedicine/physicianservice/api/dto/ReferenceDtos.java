package com.telemedicine.physicianservice.api.dto;

/** Response records for the clinical reference directory search endpoints (D-08 dropdowns). */
public final class ReferenceDtos {

    private ReferenceDtos() {}

    /** An ICD-10 diagnosis option. */
    public record DiagnosisOption(String code, String title, String system) {}

    /** A medication formulary option. */
    public record MedicationOption(String id, String name, String strength, String form) {}

    /** A diagnostic-test catalogue option. */
    public record DiagnosticTestOption(String id, String name, String category, String specimen) {}
}
