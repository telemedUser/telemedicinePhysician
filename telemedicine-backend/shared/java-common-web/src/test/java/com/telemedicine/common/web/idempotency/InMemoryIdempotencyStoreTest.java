package com.telemedicine.common.web.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.time.FixedTimeProvider;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyStoreTest {

    private final FixedTimeProvider clock = FixedTimeProvider.atEpoch();
    private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(clock);

    @Test
    @DisplayName("only the first caller wins the reservation")
    void reservationIsExclusive() {
        assertThat(store.tryReserve("k1", "fp", Duration.ofMinutes(5))).isTrue();
        assertThat(store.tryReserve("k1", "fp", Duration.ofMinutes(5))).isFalse();
    }

    @Test
    @DisplayName("concurrent reservations of the same key produce exactly one winner")
    void reservationIsAtomicUnderConcurrency() throws Exception {
        int threads = 16;
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Callable<Boolean>> tasks =
                    java.util.Collections.nCopies(
                            threads, () -> store.tryReserve("race", "fp", Duration.ofMinutes(5)));

            List<Future<Boolean>> results = pool.invokeAll(tasks);

            long winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
        }
    }

    @Test
    void storesAndReturnsTheCompletedResponse() {
        store.tryReserve("k2", "fp", Duration.ofMinutes(5));

        assertThat(store.find("k2"))
                .get()
                .satisfies(record -> assertThat(record.completed()).isFalse());

        store.complete("k2", 201, "{\"id\":1}", "application/json", Duration.ofMinutes(5));

        assertThat(store.find("k2"))
                .get()
                .satisfies(
                        record -> {
                            assertThat(record.completed()).isTrue();
                            assertThat(record.status()).isEqualTo(201);
                            assertThat(record.responseBody()).isEqualTo("{\"id\":1}");
                            assertThat(record.contentType()).isEqualTo("application/json");
                            assertThat(record.fingerprint()).isEqualTo("fp");
                        });
    }

    @Test
    void completingAnUnknownKeyIsANoOp() {
        store.complete("missing", 200, "body", "application/json", Duration.ofMinutes(5));

        assertThat(store.find("missing")).isEmpty();
    }

    @Test
    void releaseFreesTheKeyForAnotherAttempt() {
        store.tryReserve("k3", "fp", Duration.ofMinutes(5));
        store.release("k3");

        assertThat(store.find("k3")).isEmpty();
        assertThat(store.tryReserve("k3", "fp", Duration.ofMinutes(5))).isTrue();
    }

    @Test
    @DisplayName("entries stop being replayable once their TTL elapses")
    void entriesExpire() {
        store.tryReserve("k4", "fp", Duration.ofMinutes(5));
        store.complete("k4", 200, "body", "application/json", Duration.ofMinutes(5));

        clock.advance(Duration.ofMinutes(4));
        assertThat(store.find("k4")).isPresent();

        clock.advance(Duration.ofMinutes(2));
        assertThat(store.find("k4")).isEmpty();
        assertThat(store.size()).isZero();
    }

    @Test
    void expiredEntriesAreReclaimedOnReservation() {
        store.tryReserve("k5", "fp", Duration.ofMinutes(1));
        clock.advance(Duration.ofMinutes(2));

        assertThat(store.tryReserve("k5", "fp2", Duration.ofMinutes(1))).isTrue();
        assertThat(store.find("k5"))
                .get()
                .satisfies(r -> assertThat(r.fingerprint()).isEqualTo("fp2"));
    }

    @Test
    void defaultConstructorUsesTheSystemClock() {
        InMemoryIdempotencyStore systemStore = new InMemoryIdempotencyStore();

        assertThat(systemStore.tryReserve("k", "fp", Duration.ofMinutes(1))).isTrue();
        assertThat(systemStore.size()).isEqualTo(1);
    }
}
