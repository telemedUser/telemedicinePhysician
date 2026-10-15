package com.telemedicine.physicianservice.domain.provider;

/**
 * Operational status of a provider (PRD §21, TM-PRV-003). Status changes take immediate effect on
 * routing and the directory.
 */
public enum ProviderStatus {
    ACTIVE,
    SUSPENDED,
    ON_LEAVE,
    OFFBOARDED;

    public static ProviderStatus fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Provider status is required");
        }
        try {
            return ProviderStatus.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown provider status '" + value + "'");
        }
    }
}
