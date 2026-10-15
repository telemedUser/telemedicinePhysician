package com.telemedicine.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TelemedicinePersistencePropertiesTest {

    @Test
    void exposesNestedDefaults() {
        TelemedicinePersistenceProperties properties = new TelemedicinePersistenceProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getJpa().isOpenInView()).isFalse();
        assertThat(properties.getDatasource().getMaximumPoolSize()).isEqualTo(10);
        assertThat(properties.getCache().getDefaultTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getIdempotency().getStore()).isEqualTo("auto");
        assertThat(properties.getProcessedEvents().getRetention()).isEqualTo(Duration.ofDays(7));
    }
}
