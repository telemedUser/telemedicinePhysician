package com.telemedicine.identityservice.api.dto;

import java.time.Instant;

public record ValidateIdentityResponse(
        String appointmentId, String patientRef, String method, boolean validated, Instant validatedAt) {}
