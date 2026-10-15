package com.telemedicine.notificationservice.api.dto;

public record SendNotificationResponse(
        String notificationId, String channel, String templateKey, String status) {}
