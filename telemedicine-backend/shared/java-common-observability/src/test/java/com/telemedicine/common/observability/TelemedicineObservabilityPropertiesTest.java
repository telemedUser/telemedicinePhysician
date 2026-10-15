package com.telemedicine.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelemedicineObservabilityPropertiesTest {

    @Test
    void bindsNestedDefaults() {
        TelemedicineObservabilityProperties properties = new TelemedicineObservabilityProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getTracing().getOtlpEndpoint()).isEqualTo("http://jaeger:4317");
        assertThat(properties.getMetrics().getSloMillis())
                .containsExactly(100, 200, 300, 500, 1000);
        assertThat(properties.getCorrelation().getAppointmentHeader())
                .isEqualTo("X-Appointment-Id");
    }
}
