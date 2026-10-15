package com.telemedicine.common.persistence.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptedStringConverterTest {

    @Test
    void roundTripsThroughDatabaseColumnRepresentation() {
        PhiEncryptionService service = PhiEncryptionService.fromKeyMaterial(testKey(), "v1");
        EncryptedStringConverter converter = new EncryptedStringConverter(service);

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        String column = converter.convertToDatabaseColumn("patient-name");
        assertThat(column).startsWith("v1:");
        assertThat(converter.convertToEntityAttribute(column)).isEqualTo("patient-name");
    }

    private static String testKey() {
        byte[] raw = new byte[32];
        return Base64.getEncoder().encodeToString(raw);
    }
}
