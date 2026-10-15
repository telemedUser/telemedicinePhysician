package com.telemedicine.identityservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record FonasaChallengeRequest(
        @NotBlank String appointmentId,
        @NotBlank String patientId,
        @NotBlank String rut,
        @NotBlank String phone) {}
