package com.telemedicine.notificationservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record SendNotificationRequest(
        @NotBlank String templateKey,
        String locale,
        String channel,
        String appointmentId,
        @NotBlank String recipientRef,
        Map<String, String> variables) {}
