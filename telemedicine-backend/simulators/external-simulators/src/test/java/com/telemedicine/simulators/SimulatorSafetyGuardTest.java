package com.telemedicine.simulators;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SimulatorSafetyGuardTest {

    @Test
    void allowsNonProductionEnvironment() {
        assertThatCode(() -> SimulatorSafetyGuard.assertNotProduction("dev")).doesNotThrowAnyException();
        assertThatCode(() -> SimulatorSafetyGuard.assertNotProduction(null)).doesNotThrowAnyException();
    }

    @Test
    void refusesProductionEnvironment() {
        assertThatThrownBy(() -> SimulatorSafetyGuard.assertNotProduction("production"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TELEMEDICINE_ENVIRONMENT=production");
    }
}
