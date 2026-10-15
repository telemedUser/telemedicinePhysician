package com.telemedicine.identityservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record FonasaVerifyRequest(
        @NotBlank String appointmentId,
        @NotBlank String challengeId,
        @NotBlank String code,
        @NotBlank String patientId) {}
