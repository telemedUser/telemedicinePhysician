package com.telemedicine.common.integration.provider;

import com.telemedicine.common.integration.ProviderCategory;
import com.telemedicine.common.integration.ProviderRegistry;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the active provider from config-service with a cached safe default and health-based
 * failover.
 */
public class ProviderSelector<T> {

    private static final Logger log = LoggerFactory.getLogger(ProviderSelector.class);

    private final ProviderRegistry<T> registry;
    private final PlatformConfigClient configClient;
    private final ProviderCategory category;
    private final String safeDefault;
    private final ProviderHealthChecker healthChecker;
    private final List<ProviderChangedListener> listeners = new ArrayList<>();
    private volatile String lastResolved;

    public ProviderSelector(
            ProviderRegistry<T> registry,
            PlatformConfigClient configClient,
            ProviderCategory category,
            String safeDefault,
            ProviderHealthChecker healthChecker) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.configClient = Objects.requireNonNull(configClient, "configClient");
        this.category = Objects.requireNonNull(category, "category");
        this.safeDefault = safeDefault == null || safeDefault.isBlank() ? "simulator" : safeDefault;
        this.healthChecker = healthChecker == null ? name -> true : healthChecker;
    }

    public void addListener(ProviderChangedListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Returns the currently active provider, failing over when health checks fail. */
    public T active() {
        String configured = resolveConfiguredProvider();
        String selected = selectHealthyProvider(configured);
        notifyIfChanged(selected);
        registry.setActiveProvider(selected);
        return registry.active();
    }

    /** Reads the configured provider without mutating registry state. */
    public String configuredProviderName() {
        return resolveConfiguredProvider();
    }

    public ProviderCategory category() {
        return category;
    }

    private String resolveConfiguredProvider() {
        PlatformConfigSnapshot snapshot = configClient.snapshot();
        String configured =
                category == ProviderCategory.VIDEO
                        ? snapshot.videoProvider()
                        : snapshot.chatProvider();
        if (configured == null || configured.isBlank()) {
            return safeDefault;
        }
        if (!registry.listProviders().contains(configured)) {
            log.warn(
                    "Configured {} provider '{}' is not registered; using safe default '{}'",
                    category,
                    configured,
                    safeDefault);
            return safeDefault;
        }
        return configured;
    }

    private String selectHealthyProvider(String preferred) {
        if (registry.listProviders().contains(preferred) && healthChecker.isHealthy(preferred)) {
            return preferred;
        }
        log.warn(
                "Preferred {} provider '{}' is unhealthy or missing; attempting failover",
                category,
                preferred);
        if (!preferred.equals(safeDefault)
                && registry.listProviders().contains(safeDefault)
                && healthChecker.isHealthy(safeDefault)) {
            log.info(
                    "Failing over {} provider from '{}' to safe default '{}'",
                    category,
                    preferred,
                    safeDefault);
            return safeDefault;
        }
        for (String candidate : registry.listProviders()) {
            if (candidate.equals(preferred) || candidate.equals(safeDefault)) {
                continue;
            }
            if (healthChecker.isHealthy(candidate)) {
                log.info(
                        "Failing over {} provider from '{}' to '{}'",
                        category,
                        preferred,
                        candidate);
                return candidate;
            }
        }
        if (registry.listProviders().contains(safeDefault)) {
            log.warn(
                    "All {} providers unhealthy; falling back to safe default '{}'",
                    category,
                    safeDefault);
            return safeDefault;
        }
        return preferred;
    }

    private void notifyIfChanged(String current) {
        String previous = lastResolved;
        if (Objects.equals(previous, current)) {
            return;
        }
        lastResolved = current;
        for (ProviderChangedListener listener : listeners) {
            listener.onProviderChanged(category, previous, current);
        }
    }
}
