package com.telemedicine.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SoftDeletableEntityTest {

    @Test
    void softDeleteMarksDeletedAt() {
        TestEntity entity = new TestEntity();
        Instant when = Instant.parse("2026-01-01T00:00:00Z");

        entity.softDelete(when);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isEqualTo(when);
    }

    static class TestEntity extends SoftDeletableEntity {}
}
