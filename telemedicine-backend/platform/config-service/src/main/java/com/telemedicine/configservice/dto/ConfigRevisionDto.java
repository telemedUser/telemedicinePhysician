package com.telemedicine.configservice.dto;

import java.time.Instant;

public record ConfigRevisionDto(
        String id,
        String configKey,
        String previousValueHash,
        String newValueHash,
        String changedBy,
        Instant changedAt) {}
