package com.telemedicine.physicianbff.dto;

import java.util.List;

public record WorklistResponse(AggregationStatus status, List<WorklistEntry> entries) {}
