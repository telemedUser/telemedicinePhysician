package com.telemedicine.adminbff.dto;

import java.util.List;
import java.util.Map;

public record AdminConfigView(
        String videoProvider,
        String chatProvider,
        boolean telemetryEnabled,
        Map<String, Boolean> notificationChannels,
        Map<String, Boolean> mfeRemotes,
        String telemetryRulesJson) {}
