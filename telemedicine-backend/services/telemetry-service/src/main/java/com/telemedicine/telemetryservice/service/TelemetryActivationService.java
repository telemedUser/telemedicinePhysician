package com.telemedicine.telemetryservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import org.springframework.stereotype.Component;

@Component
public class TelemetryActivationService {

    private final PlatformConfigClient configClient;
    private final ObjectMapper objectMapper;

    public TelemetryActivationService(
            PlatformConfigClient configClient, ObjectMapper objectMapper) {
        this.configClient = configClient;
        this.objectMapper = objectMapper;
    }

    public void requireEnabled(String appointmentType, String specialty) {
        PlatformConfigSnapshot snapshot = configClient.snapshot();
        if (!snapshot.telemetryEnabled()) {
            throw TelemetryDisabledException.notEnabled();
        }
        if (!matchesRules(snapshot.telemetryRulesJson(), appointmentType, specialty)) {
            throw TelemetryDisabledException.notEnabled();
        }
    }

    private boolean matchesRules(String rulesJson, String appointmentType, String specialty) {
        if (rulesJson == null || rulesJson.isBlank() || "{}".equals(rulesJson.trim())) {
            return true;
        }
        try {
            JsonNode rules = objectMapper.readTree(rulesJson);
            if (rules.isEmpty()) {
                return true;
            }
            if (appointmentType != null
                    && rules.has("appointmentTypes")
                    && rules.get("appointmentTypes").isArray()
                    && !rules.get("appointmentTypes").isEmpty()) {
                boolean matched = false;
                for (JsonNode node : rules.get("appointmentTypes")) {
                    if (appointmentType.equalsIgnoreCase(node.asText())) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
            if (specialty != null
                    && rules.has("specialties")
                    && rules.get("specialties").isArray()
                    && !rules.get("specialties").isEmpty()) {
                for (JsonNode node : rules.get("specialties")) {
                    if (specialty.equalsIgnoreCase(node.asText())) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        } catch (Exception exception) {
            return true;
        }
    }
}
