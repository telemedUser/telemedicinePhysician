package com.telemedicine.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TelemedicineIntegrationPropertiesTest {

    @Test
    void exposesDefaults() {
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getConfigServiceBaseUrl()).isEqualTo("http://config-service:8112");
        assertThat(properties.getConfigCacheTtl()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.getSafeDefaultProvider()).isEqualTo("simulator");
        assertThat(properties.getSimulator().getBaseUrl()).isEqualTo("lb://external-simulators");
        assertThat(properties.getHttp().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getHttp().getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getWebhook().getTimestampWindow()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void mutatorsRoundTrip() {
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.setEnabled(false);
        properties.setSafeDefaultProvider("vonage");
        properties.getSimulator().setBaseUrl("http://localhost:8200");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getSafeDefaultProvider()).isEqualTo("vonage");
        assertThat(properties.getSimulator().getBaseUrl()).isEqualTo("http://localhost:8200");
    }
}
