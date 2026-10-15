package com.telemedicine.common.persistence.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/** JPA converter that transparently encrypts {@link String} columns at rest. */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final PhiEncryptionService encryptionService;

    public EncryptedStringConverter(PhiEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
