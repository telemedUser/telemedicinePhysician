package com.telemedicine.simulators.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VendorConfigTest {

    @Test
    void defaultsAreZeroLatencyAndEnabled() {
        VendorConfig config = VendorConfig.defaults();
        assertThat(config.latencyMs()).isZero();
        assertThat(config.errorRate()).isZero();
        assertThat(config.enabled()).isTrue();
    }

    @Test
    void propertiesSeedAllVendors() {
        SimulatorProperties properties = new SimulatorProperties();
        assertThat(properties.getVendors()).hasSize(VendorNames.ALL.length);
        for (String vendor : VendorNames.ALL) {
            assertThat(properties.getVendors()).containsKey(vendor);
        }
    }

    @Test
    void withMethodsProduceUpdatedCopy() {
        VendorConfig updated = VendorConfig.defaults().withLatencyMs(100).withErrorRate(0.5).withEnabled(false);
        assertThat(updated.latencyMs()).isEqualTo(100);
        assertThat(updated.errorRate()).isEqualTo(0.5);
        assertThat(updated.enabled()).isFalse();
    }
}
