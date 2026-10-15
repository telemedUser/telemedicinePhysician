package com.telemedicine.common.persistence.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptedInstantConverterTest {

    @Test
    void roundTripsInstantValues() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(testKey(), "v1");
        EncryptedInstantConverter converter = new EncryptedInstantConverter(service);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        Instant value = Instant.parse("2026-06-01T12:00:00Z");

        String column = converter.convertToDatabaseColumn(value);
        assertThat(converter.convertToEntityAttribute(column)).isEqualTo(value);
    }

    private static String testKey() {
        byte[] raw = new byte[32];
        return Base64.getEncoder().encodeToString(raw);
    }
}
