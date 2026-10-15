package com.telemedicine.common.persistence.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdempotencyKeyEntityTest {

    @Test
    void accessorsRoundTrip() {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setKey("key");
        entity.setFingerprint("fp");
        entity.setStatus(201);
        entity.setResponseBody("{}");
        entity.setContentType("application/json");
        entity.setCompleted(true);

        assertThat(entity.getKey()).isEqualTo("key");
        assertThat(entity.getFingerprint()).isEqualTo("fp");
        assertThat(entity.getStatus()).isEqualTo(201);
        assertThat(entity.getResponseBody()).isEqualTo("{}");
        assertThat(entity.getContentType()).isEqualTo("application/json");
        assertThat(entity.isCompleted()).isTrue();
    }
}
