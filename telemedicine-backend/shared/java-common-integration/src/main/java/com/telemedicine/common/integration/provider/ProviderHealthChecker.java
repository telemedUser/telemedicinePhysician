package com.telemedicine.common.integration.provider;

/** Reports whether a registered provider is healthy enough to receive traffic. */
@FunctionalInterface
public interface ProviderHealthChecker {

    /**
     * @param providerKey registered provider name
     * @return {@code true} when the provider can accept calls
     */
    boolean isHealthy(String providerKey);
}
