package com.telemedicine.common.core.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Identifier factory for the platform.
 *
 * <p>Pick by storage behaviour, not by habit: use {@link #newUlid()} or {@link #newUuidV7()} for
 * anything that becomes a database primary key (both sort by creation time, so inserts append
 * instead of scattering across the index), and {@link #newId()} only where an opaque random value
 * is genuinely wanted.
 */
public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {}

    /** Random UUIDv4. Retained as the original API; prefer a time-sortable id for new keys. */
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** Time-sortable 26-character ULID. Preferred for outbox, audit and event identifiers. */
    public static String newUlid() {
        return Ulid.next();
    }

    /** Time-sortable UUIDv7, for columns that must keep the {@code uuid} SQL type. */
    public static String newUuidV7() {
        return uuidV7().toString();
    }

    /** Time-sortable UUIDv7 as a {@link UUID}, per RFC 9562. */
    public static UUID uuidV7() {
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        long randA = ((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL);
        long mostSignificant = (timestamp << 16) | 0x7000L | randA;

        long randB = 0;
        for (int i = 2; i < 10; i++) {
            randB = (randB << 8) | (random[i] & 0xFFL);
        }
        // Clear the two variant bits and set them to 0b10 (RFC 4122 variant).
        long leastSignificant = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(mostSignificant, leastSignificant);
    }

    /** Correlation identifier for a journey. ULID so logs sort chronologically by id. */
    public static String correlationId() {
        return Ulid.next();
    }

    /** Event identifier used as both the outbox primary key and the Kafka message key. */
    public static String eventId() {
        return Ulid.next();
    }

    /** Server-generated idempotency key, for callers that do not supply their own. */
    public static String idempotencyKey() {
        return Ulid.next();
    }
}
