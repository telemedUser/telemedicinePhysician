package com.telemedicine.common.persistence.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class PhiEncryptionServiceTest {

    private static final String KEY_V1 = encodeKey(1);
    private static final String KEY_V2 = encodeKey(2);

    @Test
    void encryptsAndDecryptsWithActiveKey() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(KEY_V1, "v1");

        String encrypted = service.encrypt("12.345.678-9");
        assertThat(encrypted).startsWith("v1:");
        assertThat(service.decrypt(encrypted)).isEqualTo("12.345.678-9");
    }

    @Test
    void decryptsWithRotatedKeyMaterial() {
        PhiEncryptionService writer = PhiEncryptionService.fromKeyMaterial(KEY_V1, "v1");
        String encrypted = writer.encrypt("secret");

        PhiEncryptionService reader =
                PhiEncryptionService.fromKeyMaterial("v1:" + KEY_V1 + ",v2:" + KEY_V2, "v2");
        assertThat(reader.decrypt(encrypted)).isEqualTo("secret");
    }

    @Test
    void blindIndexIsDeterministicAndNormalized() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(KEY_V1, "v1");

        assertThat(service.blindIndex("12.345.678-9"))
                .isEqualTo(service.blindIndex(" 12.345.678-9 "));
    }

    @Test
    void rejectsMissingKeyMaterial() {
        assertThatThrownBy(() -> PhiEncryptionService.fromKeyMaterial("  ", "v1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handlesNullPlaintext() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(KEY_V1, "v1");
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
        assertThat(service.blindIndex(null)).isNull();
    }

    @Test
    void encryptsWithExplicitKeyId() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial("v2:" + KEY_V2, "v2");
        assertThat(service.encrypt("value", "v2")).startsWith("v2:");
    }

    @Test
    void activeKeyIdIsAvailable() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(KEY_V1, "v1");
        assertThat(service.activeKeyId()).isEqualTo("v1");
    }

    @Test
    void rejectsInvalidKeyLength() {
        assertThatThrownBy(() -> PhiEncryptionService.fromKeyMaterial("c2hvcnQ=", "v1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownKeyIdOnDecrypt() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(KEY_V1, "v1");
        String encrypted = service.encrypt("secret");
        assertThatThrownBy(() -> service.decrypt("missing:" + encrypted.substring(encrypted.indexOf(':') + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedCiphertext() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(KEY_V1, "v1");
        assertThatThrownBy(() -> service.decrypt("v1:not-valid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String encodeKey(int seed) {
        byte[] raw = new byte[32];
        raw[0] = (byte) seed;
        return Base64.getEncoder().encodeToString(raw);
    }
}
