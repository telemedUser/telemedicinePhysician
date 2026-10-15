package com.telemedicine.common.persistence.encryption;

/** HMAC blind-index helper for searchable encrypted columns such as RUT. */
public final class BlindIndexHasher {

    private BlindIndexHasher() {}

    public static String hash(PhiEncryptionService encryptionService, String plaintext) {
        return encryptionService.blindIndex(plaintext);
    }
}
