package com.telemedicine.waitingroomservice.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionLinkRequest(@NotBlank String appointmentId, String channel) {}
