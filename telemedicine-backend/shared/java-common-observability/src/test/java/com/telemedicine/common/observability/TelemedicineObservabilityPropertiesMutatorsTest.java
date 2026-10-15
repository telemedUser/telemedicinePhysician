package com.telemedicine.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TelemedicineObservabilityPropertiesMutatorsTest {

    @Test
    void supportsPropertyMutators() {
        TelemedicineObservabilityProperties properties = new TelemedicineObservabilityProperties();
        properties.setEnabled(false);
        properties.getTracing().setOtlpEndpoint("http://custom:4317");
        properties.getTracing().setSamplingProbability(0.5);
        properties.getMetrics().setPrometheusEnabled(false);
        properties.getMetrics().setSloMillis(new double[] {50, 100});
        properties.getLogging().setJsonEnabled(true);
        properties.getLogging().setPhiRedactionCacheTtl(Duration.ofSeconds(30));
        properties.getCorrelation().setAppointmentHeader("X-Custom-Appt");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getTracing().getOtlpEndpoint()).isEqualTo("http://custom:4317");
        assertThat(properties.getMetrics().getSloMillis()).containsExactly(50, 100);
        assertThat(properties.getLogging().isJsonEnabled()).isTrue();
        assertThat(properties.getCorrelation().getAppointmentHeader()).isEqualTo("X-Custom-Appt");
    }
}
