package com.telemedicine.patientbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdentityStatus(String verificationStatus, String method) {}
