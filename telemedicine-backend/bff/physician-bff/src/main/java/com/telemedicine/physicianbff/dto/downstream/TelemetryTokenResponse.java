package com.telemedicine.physicianbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryTokenResponse(
        String token, String appointmentId, String sessionId, String provider) {}
