package com.telemedicine.patientbff.dto;

public record VideoState(AggregationStatus status, boolean available, String sessionId) {}
