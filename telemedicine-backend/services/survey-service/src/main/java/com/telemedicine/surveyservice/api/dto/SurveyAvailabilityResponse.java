package com.telemedicine.surveyservice.api.dto;

public record SurveyAvailabilityResponse(String appointmentId, boolean available, boolean submitted) {}
