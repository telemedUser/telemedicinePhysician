package com.telemedicine.patientbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoSessionSummary(
        String sessionId, String appointmentId, String provider, String status) {}
