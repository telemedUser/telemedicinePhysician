package com.telemedicine.simulators.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SimulatorEngineTest {

    private SimulatorEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimulatorEngine(new SimulatorProperties());
    }

    @Test
    void disabledVendorAlwaysFails() {
        engine.updateConfig(VendorNames.HIS, VendorConfig.defaults().withEnabled(false));
        assertThatThrownBy(() -> engine.preProcess(VendorNames.HIS, "success"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void explicitFailureScenarioThrows() {
        assertThatThrownBy(() -> engine.preProcess(VendorNames.HIS, "failure"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void partialScenarioReturnsPartialOutcome() {
        ProcessOutcome outcome = engine.preProcess(VendorNames.HIS, "partial");
        assertThat(outcome).isEqualTo(ProcessOutcome.PARTIAL);
    }

    @Test
    void latencyInjectionDelaysRequest() {
        engine.updateConfig(VendorNames.HIS, VendorConfig.defaults().withLatencyMs(50));
        long started = System.currentTimeMillis();
        engine.preProcess(VendorNames.HIS, "success");
        assertThat(System.currentTimeMillis() - started).isGreaterThanOrEqualTo(50);
    }

    @Test
    void errorRateProducesFailures() {
        engine.updateConfig(VendorNames.PAYMENT, VendorConfig.defaults().withErrorRate(1.0));
        int failures = 0;
        for (int i = 0; i < 10; i++) {
            try {
                engine.preProcess(VendorNames.PAYMENT, null);
            } catch (ResponseStatusException ex) {
                failures++;
            }
        }
        assertThat(failures).isEqualTo(10);
    }

    @Test
    void resetAllRestoresDefaults() {
        engine.updateConfig(VendorNames.HIS, VendorConfig.defaults().withLatencyMs(500).withErrorRate(1.0));
        engine.resetAll();
        assertThat(engine.getConfig(VendorNames.HIS).latencyMs()).isZero();
        assertThat(engine.getConfig(VendorNames.HIS).errorRate()).isZero();
    }

    @Test
    void resilienceModeConfiguresExpectedVendors() {
        engine.applyResilienceMode();
        assertThat(engine.getConfig(VendorNames.VONAGE).errorRate()).isEqualTo(1.0);
        assertThat(engine.getConfig(VendorNames.PAYMENT).errorRate()).isEqualTo(0.20);
        assertThat(engine.getConfig(VendorNames.HIS).latencyMs()).isEqualTo(200);
    }
}
