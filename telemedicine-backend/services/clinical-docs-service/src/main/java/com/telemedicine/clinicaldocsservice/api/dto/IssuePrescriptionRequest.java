package com.telemedicine.clinicaldocsservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record IssuePrescriptionRequest(@NotBlank String medications) {}
