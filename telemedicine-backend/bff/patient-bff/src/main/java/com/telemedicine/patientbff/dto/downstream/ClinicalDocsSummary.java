package com.telemedicine.patientbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClinicalDocsSummary(String appointmentId, String downloadUrl) {}
