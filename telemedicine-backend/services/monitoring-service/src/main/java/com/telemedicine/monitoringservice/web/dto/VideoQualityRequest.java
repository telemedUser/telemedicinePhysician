package com.telemedicine.monitoringservice.web.dto;

public record VideoQualityRequest(
        String sessionId,
        String appointmentId,
        String participantRole,
        int qualityScore) {}
