package com.telemedicine.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TelemedicineOutboxPropertiesTest {

    @Test
    void exposesDefaults() {
        TelemedicineOutboxProperties properties = new TelemedicineOutboxProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getPollInterval()).isEqualTo(Duration.ofMillis(5000));
        assertThat(properties.getBatchSize()).isEqualTo(100);
        assertThat(properties.getMaxAttempts()).isEqualTo(5);
        assertThat(properties.getBackoffMultiplier()).isEqualTo(2.0);
    }
}
