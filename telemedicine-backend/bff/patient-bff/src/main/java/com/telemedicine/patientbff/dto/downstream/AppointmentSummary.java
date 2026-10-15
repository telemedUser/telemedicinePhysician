package com.telemedicine.patientbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentSummary(
        String id,
        String patientId,
        String physicianId,
        String status,
        String scheduledAt,
        String paymentStatus,
        String source) {}
