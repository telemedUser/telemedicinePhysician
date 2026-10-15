package com.telemedicine.clinicaldocsservice.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record IssueMedicalLeaveRequest(
        @Min(1) int durationDays,
        @NotBlank String diagnosis) {}
