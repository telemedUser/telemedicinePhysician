package com.telemedicine.physicianbff.dto;

public record WorklistEntry(
        String appointmentId,
        String status,
        String patientId,
        AggregationStatus sourceStatus) {}
