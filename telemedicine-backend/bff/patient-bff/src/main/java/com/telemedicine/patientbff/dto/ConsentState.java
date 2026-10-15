package com.telemedicine.patientbff.dto;

public record ConsentState(AggregationStatus status, String platform, String clinical) {}
