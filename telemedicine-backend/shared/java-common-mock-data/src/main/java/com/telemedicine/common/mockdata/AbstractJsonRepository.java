package com.telemedicine.common.mockdata;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base for {@code @Profile("mock")} repository adapters.
 * Subclasses live in each service's {@code infrastructure/mock} package — delete that package with the datastore.
 */
public abstract class AbstractJsonRepository<T> {
    protected final MockDatastoreConnection<T> connection;

    protected AbstractJsonRepository(
            MockDatastoreFactory datastore,
            String serviceName,
            String collectionFile,
            Class<T> elementType,
            Function<T, String> idExtractor) {
        this.connection = datastore.connect(serviceName, collectionFile, elementType, idExtractor);
    }

    public Optional<T> findById(String id) {
        return connection.findById(id);
    }

    public List<T> findAll() {
        return connection.findAll();
    }

    public T save(T entity) {
        return connection.save(entity);
    }

    public void reset() {
        connection.resetOverlay();
    }
}
