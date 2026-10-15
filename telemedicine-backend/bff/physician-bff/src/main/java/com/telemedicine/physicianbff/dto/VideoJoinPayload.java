package com.telemedicine.physicianbff.dto;

public record VideoJoinPayload(AggregationStatus status, String sessionId, String token) {}
