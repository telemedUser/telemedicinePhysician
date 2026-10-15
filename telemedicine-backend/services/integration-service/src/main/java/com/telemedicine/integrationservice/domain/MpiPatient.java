package com.telemedicine.integrationservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpiPatient(
        String appointmentId,
        String patientId,
        String name,
        String dateOfBirth
) {
}
