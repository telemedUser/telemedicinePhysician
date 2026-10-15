package com.telemedicine.common.testing.containers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelemedicineContainersTest {

    @Test
    void exposesSingletonContainerReferences() {
        assertThat(TelemedicineContainers.postgres()).isSameAs(TelemedicineContainers.postgres());
        assertThat(TelemedicineContainers.kafka()).isSameAs(TelemedicineContainers.kafka());
        assertThat(TelemedicineContainers.redis()).isSameAs(TelemedicineContainers.redis());
        assertThat(TelemedicineContainers.minio()).isSameAs(TelemedicineContainers.minio());
        assertThat(TelemedicineContainers.keycloak()).isSameAs(TelemedicineContainers.keycloak());
    }
}
