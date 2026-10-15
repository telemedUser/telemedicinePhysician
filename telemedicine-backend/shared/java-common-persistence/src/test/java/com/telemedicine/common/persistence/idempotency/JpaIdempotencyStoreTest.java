package com.telemedicine.common.persistence.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.time.FixedTimeProvider;
import com.telemedicine.common.web.idempotency.IdempotencyRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class JpaIdempotencyStoreTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void tryReserveIsAtomicAcrossKeys() {
        IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
        when(repository.saveAndFlush(any()))
                .thenReturn(new IdempotencyKeyEntity())
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(repository.deleteExpired(NOW)).thenReturn(0);
        JpaIdempotencyStore store = new JpaIdempotencyStore(repository, new FixedTimeProvider(NOW));

        assertThat(store.tryReserve("key-1", "fp-1", Duration.ofHours(1))).isTrue();
        assertThat(store.tryReserve("key-1", "fp-1", Duration.ofHours(1))).isFalse();
    }

    @Test
    void completeUpdatesExistingRecord() {
        IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setKey("key-2");
        entity.setFingerprint("fp-2");
        entity.setCreatedAt(NOW);
        entity.setExpiresAt(NOW.plus(Duration.ofHours(1)));
        when(repository.findById("key-2")).thenReturn(Optional.of(entity));
        JpaIdempotencyStore store = new JpaIdempotencyStore(repository, new FixedTimeProvider(NOW));

        store.complete("key-2", 201, "{\"ok\":true}", "application/json", Duration.ofHours(1));

        assertThat(entity.isCompleted()).isTrue();
        assertThat(entity.getStatus()).isEqualTo(201);
        verify(repository).save(entity);
    }

    @Test
    void findRemovesExpiredRecords() {
        IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setKey("key-3");
        entity.setExpiresAt(NOW.minusSeconds(1));
        when(repository.findById("key-3")).thenReturn(Optional.of(entity));
        JpaIdempotencyStore store = new JpaIdempotencyStore(repository, new FixedTimeProvider(NOW));

        assertThat(store.find("key-3")).isEmpty();
        verify(repository).delete(entity);
    }

    @Test
    void findReturnsStoredRecord() {
        IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setKey("key-4");
        entity.setFingerprint("fp-4");
        entity.setStatus(200);
        entity.setResponseBody("{}");
        entity.setContentType("application/json");
        entity.setCompleted(true);
        entity.setCreatedAt(NOW);
        entity.setExpiresAt(NOW.plus(Duration.ofHours(1)));
        when(repository.findById("key-4")).thenReturn(Optional.of(entity));
        JpaIdempotencyStore store = new JpaIdempotencyStore(repository, new FixedTimeProvider(NOW));

        Optional<IdempotencyRecord> found = store.find("key-4");
        assertThat(found).get().extracting(IdempotencyRecord::status).isEqualTo(200);
    }

    @Test
    void completeIgnoresMissingKey() {
        IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
        when(repository.findById("missing")).thenReturn(Optional.empty());
        JpaIdempotencyStore store = new JpaIdempotencyStore(repository, new FixedTimeProvider(NOW));

        store.complete("missing", 200, "{}", "application/json", Duration.ofHours(1));

        verify(repository, never()).save(any());
    }

    @Test
    void releaseDeletesRecord() {
        IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
        JpaIdempotencyStore store = new JpaIdempotencyStore(repository, new FixedTimeProvider(NOW));

        store.release("key-5");

        verify(repository).deleteById("key-5");
    }
}
