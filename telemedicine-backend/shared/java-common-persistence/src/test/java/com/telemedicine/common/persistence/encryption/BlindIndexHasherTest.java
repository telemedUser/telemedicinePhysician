package com.telemedicine.common.persistence.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class BlindIndexHasherTest {

    @Test
    void delegatesToEncryptionService() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(testKey(), "v1");

        assertThat(BlindIndexHasher.hash(service, "12.345.678-9"))
                .isEqualTo(service.blindIndex("12.345.678-9"));
    }

    private static String testKey() {
        byte[] raw = new byte[32];
        return Base64.getEncoder().encodeToString(raw);
    }
}
