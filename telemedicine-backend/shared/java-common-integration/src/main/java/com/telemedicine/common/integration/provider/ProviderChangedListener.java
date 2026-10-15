package com.telemedicine.common.integration.provider;

/** Callback invoked when the active provider changes after config refresh or health failover. */
@FunctionalInterface
public interface ProviderChangedListener {

    /**
     * @param category provider family that changed
     * @param previous previously active provider key, or {@code null} on first resolution
     * @param current newly active provider key
     */
    void onProviderChanged(
            com.telemedicine.common.integration.ProviderCategory category,
            String previous,
            String current);
}
