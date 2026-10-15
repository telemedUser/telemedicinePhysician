package com.telemedicine.clinicaldocsservice.api.dto;

import java.util.List;

public record AppointmentDocumentsResponse(
        String appointmentId,
        String downloadUrl,
        List<DocumentSummary> documents,
        List<PrescriptionSummary> prescriptions,
        List<MedicalLeaveSummary> medicalLeaves) {

    public record DocumentSummary(String documentId, String documentType, String issuedAt) {}

    public record PrescriptionSummary(String prescriptionId, String provider, String issuedAt) {}

    public record MedicalLeaveSummary(String medicalLeaveId, int durationDays, String issuedAt) {}
}
