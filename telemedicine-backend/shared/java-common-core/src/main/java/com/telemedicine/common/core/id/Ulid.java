package com.telemedicine.common.core.id;

import java.security.SecureRandom;

/**
 * Monotonic ULID generator: a 48-bit millisecond timestamp followed by 80 bits of randomness,
 * rendered as 26 Crockford base-32 characters.
 *
 * <p>ULIDs sort lexicographically in creation order, which matters because {@code outbox_events}
 * and the audit tables are read in insertion order and indexed on their primary key. Random UUIDv4
 * keys scatter inserts across the whole B-tree and fragment it; ULIDs append.
 *
 * <p>Monotonicity is preserved within a millisecond by incrementing the random component, and
 * across a backwards clock step by holding the last timestamp.
 */
public final class Ulid {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int LENGTH = 26;
    private static final int RANDOM_BYTES = 10;
    private static final long MAX_TIMESTAMP = (1L << 48) - 1;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Object LOCK = new Object();
    private static final byte[] RANDOMNESS = new byte[RANDOM_BYTES];

    private static long lastTimestamp = -1L;

    private Ulid() {}

    /** Generates the next ULID, guaranteed to sort after every ULID previously returned here. */
    public static String next() {
        return next(System.currentTimeMillis());
    }

    static String next(long timestamp) {
        synchronized (LOCK) {
            if (timestamp > lastTimestamp) {
                lastTimestamp = timestamp;
                RANDOM.nextBytes(RANDOMNESS);
            } else if (!increment()) {
                // Randomness exhausted inside a single millisecond, or the clock stepped
                // backwards: borrow from the next millisecond rather than emit a duplicate.
                lastTimestamp++;
                RANDOM.nextBytes(RANDOMNESS);
            }
            if (lastTimestamp > MAX_TIMESTAMP) {
                throw new IllegalStateException("ULID timestamp overflowed its 48-bit range");
            }
            return encode(lastTimestamp, RANDOMNESS);
        }
    }

    /** Adds one to the 80-bit randomness. Returns false when it wrapped past its maximum. */
    private static boolean increment() {
        for (int i = RANDOM_BYTES - 1; i >= 0; i--) {
            if (RANDOMNESS[i] != (byte) 0xFF) {
                RANDOMNESS[i]++;
                return true;
            }
            RANDOMNESS[i] = 0;
        }
        return false;
    }

    private static String encode(long timestamp, byte[] randomness) {
        char[] out = new char[LENGTH];

        // 48 timestamp bits occupy the first 10 characters (50 bits, top 2 always zero).
        for (int i = 0; i < 10; i++) {
            out[9 - i] = ALPHABET[(int) ((timestamp >>> (5 * i)) & 0x1F)];
        }

        // 80 randomness bits split into two exact 40-bit halves of 8 characters each.
        long high = toLong(randomness, 0);
        long low = toLong(randomness, 5);
        for (int i = 0; i < 8; i++) {
            out[17 - i] = ALPHABET[(int) ((high >>> (5 * i)) & 0x1F)];
            out[25 - i] = ALPHABET[(int) ((low >>> (5 * i)) & 0x1F)];
        }
        return new String(out);
    }

    /** Reads five bytes starting at {@code offset} as a big-endian 40-bit value. */
    private static long toLong(byte[] bytes, int offset) {
        long value = 0;
        for (int i = 0; i < 5; i++) {
            value = (value << 8) | (bytes[offset + i] & 0xFFL);
        }
        return value;
    }

    /** True when the supplied string is a syntactically valid ULID. */
    public static boolean isValid(String candidate) {
        if (candidate == null || candidate.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < LENGTH; i++) {
            if (indexOf(candidate.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(char c) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
