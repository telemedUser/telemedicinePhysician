package com.telemedicine.common.integration.config;

import java.util.Map;

/** Cached platform configuration snapshot. */
public record PlatformConfigSnapshot(
        String videoProvider,
        String chatProvider,
        boolean telemetryEnabled,
        Map<String, Boolean> notificationChannels,
        Map<String, Boolean> mfeRemotes,
        String telemetryRulesJson,
        java.time.Instant refreshedAt) {

    public static PlatformConfigSnapshot defaults() {
        return new PlatformConfigSnapshot(
                "simulator",
                "simulator",
                true,
                Map.of("email", true, "sms", true, "whatsapp", false, "push", true),
                Map.of(),
                "{}",
                java.time.Instant.now());
    }
}
