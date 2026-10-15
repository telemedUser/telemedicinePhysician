package com.telemedicine.configservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.ValidationException;
import java.util.Set;

final class ConfigKeyValidator {

    private static final Set<String> PROVIDERS = Set.of("simulator", "vonage", "acs");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ConfigKeyValidator() {}

    static void validate(String key, String value) {
        switch (key) {
            case "video.provider", "chat.provider" -> validateProvider(key, value);
            case "telemetry.enabled" -> validateBoolean(key, value);
            case "telemetry.activation.rules" -> validateJsonObject(key, value);
            default -> {
                if (key.startsWith("notification.") && key.endsWith(".enabled")) {
                    validateBoolean(key, value);
                } else if (key.startsWith("mfe.") && key.endsWith(".enabled")) {
                    validateBoolean(key, value);
                } else {
                    throw new ValidationException(
                            ErrorCode.CONFIG_VALUE_INVALID, "Unsupported configuration key: " + key);
                }
            }
        }
    }

    private static void validateProvider(String key, String value) {
        if (value == null || !PROVIDERS.contains(value.trim())) {
            throw ValidationException.field(
                    key, "must be one of: simulator, vonage, acs");
        }
    }

    private static void validateBoolean(String key, String value) {
        if (value == null) {
            throw ValidationException.field(key, "must be true or false");
        }
        String normalized = value.trim().toLowerCase();
        if (!normalized.equals("true") && !normalized.equals("false")) {
            throw ValidationException.field(key, "must be true or false");
        }
    }

    private static void validateJsonObject(String key, String value) {
        if (value == null || value.isBlank()) {
            throw ValidationException.field(key, "must be valid JSON");
        }
        try {
            JsonNode node = MAPPER.readTree(value);
            if (!node.isObject()) {
                throw ValidationException.field(key, "must be a JSON object");
            }
        } catch (ValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ValidationException.field(key, "must be valid JSON");
        }
    }
}
