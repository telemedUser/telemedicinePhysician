package com.telemedicine.physicianservice.api.dto;

import java.util.List;

/**
 * Full provider detail (PRD §21). Matches the physician frontend {@code ProviderDetail} shape:
 * extends the summary with credentials, availability, productivity metrics and projection
 * freshness (TM-ARC-020).
 */
public record ProviderDetailResponse(
        String providerId,
        String displayName,
        String status,
        List<String> specialties,
        List<String> languages,
        List<String> modes,
        List<Credential> credentials,
        List<AvailabilityWindow> availability,
        Metrics metrics,
        String sourceRef,
        String lastRefreshedAt,
        boolean stale) {

    public record Credential(String type, String number, String validUntil, boolean verified) {}

    public record AvailabilityWindow(
            int dayOfWeek, String startTime, String endTime, String mode, int maxConcurrent) {}

    public record Metrics(
            double utilization,
            long avgDurationSeconds,
            long avgDocumentationSeconds,
            double noShowRate,
            double technicalFailureRate) {}
}
