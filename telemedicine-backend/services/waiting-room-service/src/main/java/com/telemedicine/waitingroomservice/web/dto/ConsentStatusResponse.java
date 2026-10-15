package com.telemedicine.waitingroomservice.web.dto;

import java.util.Map;

public record ConsentStatusResponse(
        boolean granted, String reason, String requiredVersion, String acceptedVersion) {

    public static ConsentStatusResponse consented() {
        return new ConsentStatusResponse(true, null, null, null);
    }

    public static ConsentStatusResponse missing() {
        return new ConsentStatusResponse(false, "CONSENT_REQUIRED", null, null);
    }

    public static ConsentStatusResponse withdrawn() {
        return new ConsentStatusResponse(false, "CONSENT_WITHDRAWN", null, null);
    }

    public static ConsentStatusResponse outdated(String requiredVersion) {
        return new ConsentStatusResponse(false, "CONSENT_VERSION_OUTDATED", requiredVersion, null);
    }

    public Map<String, Object> toMap() {
        if (granted) {
            return Map.of("granted", true);
        }
        if (requiredVersion != null) {
            return Map.of("granted", false, "reason", reason, "requiredVersion", requiredVersion);
        }
        return Map.of("granted", false, "reason", reason);
    }
}
