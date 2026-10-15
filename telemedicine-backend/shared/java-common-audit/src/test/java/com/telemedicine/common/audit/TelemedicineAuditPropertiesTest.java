package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelemedicineAuditPropertiesTest {

    @Test
    void defaultsAreProductionSafe() {
        TelemedicineAuditProperties properties = new TelemedicineAuditProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getSystemActorId()).isEqualTo("system");
        assertThat(properties.getSystemActorRole()).isEqualTo("SYSTEM");
        assertThat(properties.isPhiSafeByDefault()).isTrue();
    }
}
