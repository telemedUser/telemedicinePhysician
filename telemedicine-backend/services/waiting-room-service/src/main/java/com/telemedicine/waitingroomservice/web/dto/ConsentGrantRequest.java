package com.telemedicine.waitingroomservice.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsentGrantRequest(@NotBlank String appointmentId, String version) {}
