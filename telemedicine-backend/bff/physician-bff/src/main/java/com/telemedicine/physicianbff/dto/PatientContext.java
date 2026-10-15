package com.telemedicine.physicianbff.dto;

public record PatientContext(
        AggregationStatus status, String patientId, String name, String source) {}
