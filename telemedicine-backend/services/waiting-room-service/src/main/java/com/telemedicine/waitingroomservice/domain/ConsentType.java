package com.telemedicine.waitingroomservice.domain;

public enum ConsentType {
    PLATFORM_TERMS,
    CLINICAL_INFORMED;

    public static ConsentType fromWire(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Consent type is required");
        }
        return ConsentType.valueOf(value.trim().toUpperCase());
    }
}
