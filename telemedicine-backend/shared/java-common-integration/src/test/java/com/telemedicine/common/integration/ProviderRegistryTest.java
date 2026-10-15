package com.telemedicine.common.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProviderRegistryTest {

    @Test
    void registersAndResolvesActiveProvider() {
        ProviderRegistry<String> registry = new ProviderRegistry<>();
        registry.register("simulator", "sim");
        registry.register("vonage", "vonage-client");
        registry.setActiveProvider("vonage");
        assertThat(registry.active()).isEqualTo("vonage-client");
        assertThat(registry.listProviders()).containsExactlyInAnyOrder("simulator", "vonage");
    }

    @Test
    void rejectsUnknownProvider() {
        ProviderRegistry<String> registry = new ProviderRegistry<>();
        registry.register("simulator", "sim");
        assertThatThrownBy(() -> registry.setActiveProvider("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsToSimulator() {
        ProviderRegistry<String> registry = new ProviderRegistry<>();
        registry.register("simulator", "sim");
        assertThat(registry.active()).isEqualTo("sim");
    }
}
