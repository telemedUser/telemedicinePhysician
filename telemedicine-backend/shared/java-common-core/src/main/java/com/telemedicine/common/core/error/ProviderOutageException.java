package com.telemedicine.common.core.error;

import java.util.Map;

/**
 * A pluggable provider is unavailable. Distinct from {@link IntegrationException} because the
 * platform can react by failing over to another provider (Vonage to ACS, for example) rather than
 * simply surfacing the fault.
 */
public class ProviderOutageException extends DomainException {

    private final String provider;

    public ProviderOutageException(String provider, String message) {
        this(ErrorCode.PROVIDER_OUTAGE, provider, message, null);
    }

    public ProviderOutageException(ErrorCode code, String provider, String message) {
        this(code, provider, message, null);
    }

    public ProviderOutageException(
            ErrorCode code, String provider, String message, Throwable cause) {
        super(code, message, Map.of("provider", provider), cause);
        this.provider = provider;
    }

    /** Registered provider key, for example {@code vonage}, {@code acs} or {@code simulator}. */
    public String getProvider() {
        return provider;
    }
}
