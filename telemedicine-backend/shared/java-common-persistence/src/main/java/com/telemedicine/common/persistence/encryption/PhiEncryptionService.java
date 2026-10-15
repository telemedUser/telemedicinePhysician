package com.telemedicine.common.persistence.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM encryption for PHI columns with key-id prefix for rotation.
 *
 * <p>Wire format: {@code keyId:base64(nonce + ciphertext + tag)}. Keys are loaded from {@code
 * TELEMEDICINE_PHI_KEY} as {@code keyId:base64Key} pairs separated by commas, or a single base64
 * key using the configured active key id.
 */
public class PhiEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Map<String, SecretKey> keys;
    private final String activeKeyId;
    private final SecretKey blindIndexKey;

    public PhiEncryptionService(Map<String, SecretKey> keys, String activeKeyId) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("At least one PHI encryption key is required");
        }
        this.keys = Map.copyOf(keys);
        this.activeKeyId =
                activeKeyId == null || activeKeyId.isBlank()
                        ? keys.keySet().iterator().next()
                        : activeKeyId;
        if (!this.keys.containsKey(this.activeKeyId)) {
            throw new IllegalArgumentException("Active PHI key id not found: " + this.activeKeyId);
        }
        this.blindIndexKey = deriveBlindIndexKey(this.keys.get(this.activeKeyId));
    }

    public static PhiEncryptionService fromKeyMaterial(String keyMaterial, String activeKeyId) {
        Map<String, SecretKey> parsed = parseKeys(keyMaterial, activeKeyId);
        return new PhiEncryptionService(parsed, activeKeyId);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        return encrypt(plaintext, activeKeyId);
    }

    public String encrypt(String plaintext, String keyId) {
        if (plaintext == null) {
            return null;
        }
        SecretKey key = requireKey(keyId);
        try {
            byte[] iv = randomBytes(GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return keyId + ':' + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PHI encryption failed", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        int separator = ciphertext.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException("PHI ciphertext must start with a key id prefix");
        }
        String keyId = ciphertext.substring(0, separator);
        SecretKey key = requireKey(keyId);
        byte[] combined = Base64.getDecoder().decode(ciphertext.substring(separator + 1));
        if (combined.length <= GCM_IV_BYTES) {
            throw new IllegalArgumentException("PHI ciphertext is too short");
        }
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] encrypted = new byte[combined.length - GCM_IV_BYTES];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_BYTES);
        System.arraycopy(combined, GCM_IV_BYTES, encrypted, 0, encrypted.length);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PHI decryption failed", exception);
        }
    }

    /** Deterministic HMAC-SHA256 blind index for equality search on encrypted columns. */
    public String blindIndex(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(blindIndexKey);
            byte[] digest = mac.doFinal(normalize(plaintext).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("PHI blind index failed", exception);
        }
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    private SecretKey requireKey(String keyId) {
        SecretKey key = keys.get(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Unknown PHI key id: " + keyId);
        }
        return key;
    }

    private static Map<String, SecretKey> parseKeys(String keyMaterial, String activeKeyId) {
        if (keyMaterial == null || keyMaterial.isBlank()) {
            throw new IllegalArgumentException(
                    "TELEMEDICINE_PHI_KEY is required for PHI encryption");
        }
        Map<String, SecretKey> parsed = new LinkedHashMap<>();
        for (String entry : keyMaterial.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.indexOf(':');
            if (separator > 0) {
                parsed.put(
                        trimmed.substring(0, separator),
                        decodeKey(trimmed.substring(separator + 1)));
            } else {
                String keyId =
                        activeKeyId == null || activeKeyId.isBlank() ? "v1" : activeKeyId.trim();
                parsed.put(keyId, decodeKey(trimmed));
            }
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("No PHI keys parsed from TELEMEDICINE_PHI_KEY");
        }
        return parsed;
    }

    private static SecretKey decodeKey(String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalArgumentException("PHI key must decode to 32 bytes for AES-256");
        }
        return new SecretKeySpec(raw, "AES");
    }

    private static SecretKey deriveBlindIndexKey(SecretKey encryptionKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("telemedicine-blind-index".getBytes(StandardCharsets.UTF_8));
            digest.update(encryptionKey.getEncoded());
            return new SecretKeySpec(digest.digest(), "AES");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not derive blind index key", exception);
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase();
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new java.security.SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
