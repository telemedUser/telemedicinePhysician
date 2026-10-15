package com.telemedicine.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    @Test
    void prePersistAndPreUpdateMaintainTimestamps() {
        TestEntity entity = new TestEntity();
        entity.prePersist();
        Instant created = entity.getCreatedAt();
        assertThat(entity.getUpdatedAt()).isEqualTo(created);

        entity.preUpdate();
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(created);
    }

    static class TestEntity extends BaseEntity {}
}
