package com.telemedicine.common.mockdata;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Mock datastore connection — use like {@code java.sql.Connection}.
 * Services request data through this interface; JSON file location is an implementation detail.
 * Delete mock implementations when switching to Postgres JPA repositories.
 */
public interface MockDatastoreConnection<T> extends AutoCloseable {

    String collectionName();

    Optional<T> findById(String id);

    List<T> findAll();

    List<T> query(Predicate<T> predicate);

    T save(T entity);

    boolean deleteById(String id);

    /** Clears in-memory overlay; seed files unchanged. */
    void resetOverlay();

    @Override
    default void close() {
        // In-process connection — no resources to release
    }
}
