package com.telemedicine.physicianservice.api.dto;

import java.util.List;

/**
 * Provider list row (PRD §21). Matches the physician frontend {@code ProviderSummary} shape.
 */
public record ProviderSummaryResponse(
        String providerId,
        String displayName,
        String status,
        List<String> specialties,
        List<String> languages,
        List<String> modes) {}
