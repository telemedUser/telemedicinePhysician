package com.telemedicine.surveyservice.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SubmitSurveyResponseRequest(
        @Min(0) @Max(10) int nps,
        String comment) {}
