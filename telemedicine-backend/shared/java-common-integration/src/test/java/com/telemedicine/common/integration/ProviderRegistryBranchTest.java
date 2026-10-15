package com.telemedicine.common.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProviderRegistryBranchTest {

    @Test
    void throwsWhenActiveProviderMissing() {
        ProviderRegistry<String> registry = new ProviderRegistry<>();

        assertThatThrownBy(() -> registry.setActiveProvider("missing"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(registry::active).isInstanceOf(IllegalStateException.class);
    }
}
