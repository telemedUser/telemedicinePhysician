package com.telemedicine.common.persistence.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** JPA converter that transparently encrypts {@link Instant} columns at rest. */
@Component
@Converter
public class EncryptedInstantConverter implements AttributeConverter<Instant, String> {

    private final PhiEncryptionService encryptionService;

    public EncryptedInstantConverter(PhiEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService.encrypt(attribute.toString());
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Instant.parse(encryptionService.decrypt(dbData));
    }
}
