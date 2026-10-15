package com.telemedicine.common.core.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UlidTest {

    /**
     * The generator keeps a monotonic high-water mark in static state. Without resetting it, a test
     * that generates from the real clock leaves the mark in 2026 and every fixed-timestamp test
     * afterwards silently takes the clock-regression branch instead of the one it means to cover.
     */
    @BeforeEach
    void resetGeneratorState() throws Exception {
        Field field = Ulid.class.getDeclaredField("lastTimestamp");
        field.setAccessible(true);
        field.setLong(null, -1L);
    }

    @Test
    void hasTheCanonicalShape() {
        String ulid = Ulid.next();

        assertThat(ulid).hasSize(26).matches("[0-9A-HJKMNP-TV-Z]{26}");
        assertThat(Ulid.isValid(ulid)).isTrue();
    }

    @Test
    @DisplayName("ids generated inside one millisecond still sort in creation order")
    void isMonotonicWithinASingleMillisecond() {
        List<String> generated = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            generated.add(Ulid.next(1_700_000_000_000L));
        }

        assertThat(generated).doesNotHaveDuplicates();
        assertThat(generated).isSorted();
    }

    @Test
    @DisplayName("a backwards clock step never produces an id that sorts before its predecessor")
    void survivesClockRegression() {
        String before = Ulid.next(1_700_000_100_000L);
        String after = Ulid.next(1_700_000_000_000L);

        assertThat(after).isGreaterThan(before);
    }

    @Test
    @DisplayName("exhausting the random component borrows from the next millisecond")
    void handlesRandomnessOverflow() throws Exception {
        long timestamp = 1_700_000_200_000L;
        Ulid.next(timestamp);
        exhaustRandomness();

        String overflowed = Ulid.next(timestamp);

        // The timestamp prefix must have advanced rather than the id repeating.
        assertThat(overflowed).hasSize(26);
        assertThat(decodeTimestamp(overflowed)).isEqualTo(timestamp + 1);
    }

    @Test
    void timestampPrefixEncodesTheSuppliedInstant() {
        long timestamp = 1_700_000_300_000L;

        assertThat(decodeTimestamp(Ulid.next(timestamp))).isEqualTo(timestamp);
    }

    @Test
    void generatesDistinctValuesAcrossThreads() throws Exception {
        var results = new TreeSet<String>();
        var threads = new ArrayList<Thread>();
        var collected = java.util.Collections.synchronizedList(new ArrayList<String>());

        for (int t = 0; t < 8; t++) {
            Thread thread =
                    new Thread(
                            () -> {
                                for (int i = 0; i < 500; i++) {
                                    collected.add(Ulid.next());
                                }
                            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        results.addAll(collected);

        assertThat(results).hasSize(collected.size());
    }

    @Test
    void rejectsMalformedValues() {
        assertThat(Ulid.isValid(null)).isFalse();
        assertThat(Ulid.isValid("")).isFalse();
        assertThat(Ulid.isValid("TOO-SHORT")).isFalse();
        // I, L, O and U are excluded from Crockford base 32.
        assertThat(Ulid.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAI")).isFalse();
    }

    private static void exhaustRandomness() throws Exception {
        Field field = Ulid.class.getDeclaredField("RANDOMNESS");
        field.setAccessible(true);
        byte[] randomness = (byte[]) field.get(null);
        java.util.Arrays.fill(randomness, (byte) 0xFF);
    }

    private static long decodeTimestamp(String ulid) {
        String alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
        long value = 0;
        for (int i = 0; i < 10; i++) {
            value = (value << 5) | alphabet.indexOf(ulid.charAt(i));
        }
        return value;
    }
}
