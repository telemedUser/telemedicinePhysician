package com.telemedicine.configservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateConfigRequest(@NotBlank String value) {}
