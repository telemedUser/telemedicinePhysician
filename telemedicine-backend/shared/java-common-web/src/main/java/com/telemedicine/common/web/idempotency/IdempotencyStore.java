package com.telemedicine.common.web.idempotency;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage behind {@link IdempotencyKeyFilter}.
 *
 * <p>Web ships only an in-memory implementation, which is correct for a single instance and useless
 * across replicas. {@code java-common-persistence} contributes the JPA and Redis implementations
 * that make the guarantee hold in a real deployment.
 */
public interface IdempotencyStore {

    /**
     * Atomically claims the key for a first attempt.
     *
     * <p>Must be a genuine compare-and-set: two replicas processing a duplicated payment request at
     * the same instant is exactly the case this exists to prevent, so a read-then-write
     * implementation would defeat the purpose.
     *
     * @return true if this caller won the claim and should execute the request
     */
    boolean tryReserve(String key, String fingerprint, Duration ttl);

    Optional<IdempotencyRecord> find(String key);

    /** Stores the outcome so a later replay of the same key can return it without re-executing. */
    void complete(String key, int status, String responseBody, String contentType, Duration ttl);

    /**
     * Drops a reservation whose request failed before producing a storable response, letting the
     * client retry with the same key rather than being locked out by a transient fault.
     */
    void release(String key);
}
