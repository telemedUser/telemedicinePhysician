package com.telemedicine.configservice.dto;

import java.time.Instant;
import java.util.Map;

/** Typed platform configuration snapshot returned by {@code GET /api/v1/config}. */
public record ConfigSnapshotDto(
        String videoProvider,
        String chatProvider,
        boolean telemetryEnabled,
        Map<String, Boolean> notificationChannels,
        Map<String, Boolean> mfeRemotes,
        String telemetryRules,
        Instant refreshedAt) {}
