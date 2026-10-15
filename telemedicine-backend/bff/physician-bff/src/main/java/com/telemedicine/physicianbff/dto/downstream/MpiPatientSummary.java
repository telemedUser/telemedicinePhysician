package com.telemedicine.physicianbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpiPatientSummary(
        String appointmentId, String patientId, String name, String dateOfBirth) {}
