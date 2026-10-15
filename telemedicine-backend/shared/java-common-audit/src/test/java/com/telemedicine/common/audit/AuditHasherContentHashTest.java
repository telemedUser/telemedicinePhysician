package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditHasherContentHashTest {

    @Test
    void contentHashFallsBackWhenJacksonConversionFails() {
        Object value =
                new Object() {
                    @SuppressWarnings("unused")
                    public Object self = this;
                };
        assertThat(AuditHasher.contentHash(value)).hasSize(64);
    }
}
