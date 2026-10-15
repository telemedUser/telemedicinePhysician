package com.telemedicine.clinicaldocsservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueDocumentRequest(
        @NotBlank String appointmentId,
        @NotBlank String documentType,
        String diagnosis) {}
