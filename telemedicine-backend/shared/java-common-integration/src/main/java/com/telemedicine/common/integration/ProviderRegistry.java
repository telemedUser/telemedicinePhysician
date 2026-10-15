package com.telemedicine.common.integration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderRegistry<T> {
    private final Map<String, T> providers = new ConcurrentHashMap<>();
    private volatile String activeProvider = "simulator";

    public void register(String name, T provider) {
        providers.put(name, provider);
    }

    public T active() {
        T provider = providers.get(activeProvider);
        if (provider == null) {
            throw new IllegalStateException("No provider registered: " + activeProvider);
        }
        return provider;
    }

    public void setActiveProvider(String name) {
        if (!providers.containsKey(name)) {
            throw new IllegalArgumentException("Unknown provider: " + name);
        }
        activeProvider = name;
    }

    public List<String> listProviders() {
        return List.copyOf(providers.keySet());
    }
}
