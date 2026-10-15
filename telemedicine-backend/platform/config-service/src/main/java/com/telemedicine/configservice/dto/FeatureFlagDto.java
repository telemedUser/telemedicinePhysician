package com.telemedicine.configservice.dto;

import java.time.Instant;

public record FeatureFlagDto(String flagKey, boolean enabled, String payloadJson, Instant updatedAt) {}
