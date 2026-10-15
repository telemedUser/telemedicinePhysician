package com.telemedicine.physicianbff.dto;

public record ConsultationResponse(
        String appointmentId,
        AggregationStatus status,
        VideoJoinPayload video,
        ChatContext chat,
        TelemetryContext telemetry,
        PatientContext patientContext) {}
