package com.telemedicine.identityservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateIdentityRequest(
        @NotBlank String appointmentId, @NotBlank String patientId) {}
