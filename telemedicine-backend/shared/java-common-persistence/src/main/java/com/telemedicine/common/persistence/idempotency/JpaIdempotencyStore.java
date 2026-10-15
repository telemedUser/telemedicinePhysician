package com.telemedicine.common.persistence.idempotency;

import com.telemedicine.common.core.time.TimeProvider;
import com.telemedicine.common.web.idempotency.IdempotencyRecord;
import com.telemedicine.common.web.idempotency.IdempotencyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** JPA-backed {@link IdempotencyStore} for multi-replica idempotent POST handling. */
public class JpaIdempotencyStore implements IdempotencyStore {

    private final IdempotencyKeyRepository repository;
    private final TimeProvider clock;

    public JpaIdempotencyStore(IdempotencyKeyRepository repository, TimeProvider clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean tryReserve(String key, String fingerprint, Duration ttl) {
        purgeExpired(clock.now());
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setKey(key);
        entity.setFingerprint(fingerprint);
        entity.setStatus(0);
        entity.setCompleted(false);
        entity.setCreatedAt(clock.now());
        entity.setExpiresAt(clock.now().plus(ttl));
        try {
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> find(String key) {
        return repository.findById(key).flatMap(this::toRecord);
    }

    @Override
    @Transactional
    public void complete(
            String key, int status, String responseBody, String contentType, Duration ttl) {
        repository
                .findById(key)
                .ifPresent(
                        entity -> {
                            entity.setStatus(status);
                            entity.setResponseBody(responseBody);
                            entity.setContentType(contentType);
                            entity.setCompleted(true);
                            entity.setExpiresAt(clock.now().plus(ttl));
                            repository.save(entity);
                        });
    }

    @Override
    @Transactional
    public void release(String key) {
        repository.deleteById(key);
    }

    private Optional<IdempotencyRecord> toRecord(IdempotencyKeyEntity entity) {
        if (entity.getExpiresAt().isBefore(clock.now())) {
            repository.delete(entity);
            return Optional.empty();
        }
        return Optional.of(
                new IdempotencyRecord(
                        entity.getKey(),
                        entity.getFingerprint(),
                        entity.getStatus(),
                        entity.getResponseBody(),
                        entity.getContentType(),
                        entity.isCompleted(),
                        entity.getCreatedAt()));
    }

    private void purgeExpired(Instant now) {
        repository.deleteExpired(now);
    }
}
