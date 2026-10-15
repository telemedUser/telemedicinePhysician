package com.telemedicine.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.telemedicine.common.events.EventObjectMappers;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** SHA-256 hash chain for tamper-evident audit trails. */
public final class AuditHasher {

    private static final ObjectMapper MAPPER =
            EventObjectMappers.configure(new ObjectMapper())
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private AuditHasher() {}

    /**
     * Computes the tamper-evident link hash over the canonical audit fields consumed by {@code
     * audit-service} verification.
     */
    public static String hash(
            String actorRef,
            String action,
            String resourceType,
            String resourceRef,
            String outcome,
            Instant occurredAt,
            String previousHash) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("actorRef", actorRef);
        canonical.put("action", action);
        canonical.put("resourceType", resourceType);
        canonical.put("resourceRef", resourceRef);
        canonical.put("outcome", outcome);
        canonical.put("occurredAt", occurredAt);
        canonical.put("previousHash", normalizePreviousHash(previousHash));
        return digest(canonical);
    }

    /** SHA-256 digest of an arbitrary value serialized to canonical JSON. */
    @SuppressWarnings("unchecked")
    public static String contentHash(Object value) {
        if (value == null) {
            Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("value", null);
            return digest(canonical);
        }
        try {
            Map<String, Object> canonical = MAPPER.convertValue(value, Map.class);
            return digest(canonical);
        } catch (IllegalArgumentException exception) {
            return digest(Map.of("value", String.valueOf(value)));
        }
    }

    /** Verifies that {@code events} form an intact hash chain in sequence order. */
    public static void verifyChain(List<AuditEvent> events) {
        String previousHash = "";
        long expectedSequence = 1L;

        for (AuditEvent event : events) {
            if (event.sequence() != expectedSequence) {
                throw tamperDetected(
                        "Sequence gap at "
                                + event.auditId()
                                + ": expected "
                                + expectedSequence
                                + " got "
                                + event.sequence());
            }
            if (!Objects.equals(normalizePreviousHash(event.previousHash()), previousHash)) {
                throw tamperDetected("Previous hash mismatch at sequence " + event.sequence());
            }

            String expectedHash =
                    hash(
                            event.actorId(),
                            event.action(),
                            event.resourceType(),
                            event.resourceId(),
                            event.outcomeName(),
                            event.occurredAt(),
                            previousHash);
            if (!Objects.equals(expectedHash, event.hash())) {
                throw tamperDetected("Hash mismatch at sequence " + event.sequence());
            }

            previousHash = event.hash();
            expectedSequence++;
        }
    }

    /** Returns false when any link in the chain fails verification. */
    public static boolean isChainIntact(List<AuditEvent> events) {
        try {
            verifyChain(events);
            return true;
        } catch (AuditChainTamperException exception) {
            return false;
        }
    }

    static String normalizePreviousHash(String previousHash) {
        return previousHash == null ? "" : previousHash;
    }

    private static String digest(Map<String, Object> canonical) {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(canonical);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Audit hash computation failed", exception);
        }
    }

    private static String digest(Object value) {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(value);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Audit content hash computation failed", exception);
        }
    }

    private static String toHex(byte[] digest) {
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private static AuditChainTamperException tamperDetected(String detail) {
        return new AuditChainTamperException(detail);
    }
}
