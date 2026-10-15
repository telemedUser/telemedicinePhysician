package com.telemedicine.common.mockdata;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
class JsonMockDatastoreConnection<T> implements MockDatastoreConnection<T> {
    private final String collectionName;
    private final JsonStore<T> store;

    JsonMockDatastoreConnection(String collectionName, JsonStore<T> store) {
        this.collectionName = collectionName;
        this.store = store;
    }

    @Override
    public String collectionName() {
        return collectionName;
    }

    @Override
    public Optional<T> findById(String id) {
        return store.findById(id);
    }

    @Override
    public List<T> findAll() {
        return store.findAll();
    }

    @Override
    public List<T> query(Predicate<T> predicate) {
        return store.findBy(predicate);
    }

    @Override
    public T save(T entity) {
        return store.save(entity);
    }

    @Override
    public boolean deleteById(String id) {
        return store.deleteById(id);
    }

    @Override
    public void resetOverlay() {
        store.reset();
    }
}

@Component
public class JsonMockDatastoreFactory implements MockDatastoreFactory {
    private final org.springframework.core.io.ResourceLoader resourceLoader;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final DatastoreProperties properties;
    private final Map<String, MockDatastoreConnection<?>> connections = new ConcurrentHashMap<>();

    JsonMockDatastoreFactory(
            org.springframework.core.io.ResourceLoader resourceLoader,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            DatastoreProperties properties) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MockDatastoreConnection<T> connect(
            String serviceName,
            String collectionFile,
            Class<T> elementType,
            Function<T, String> idExtractor) {
        String key = serviceName + "/" + collectionFile;
        return (MockDatastoreConnection<T>) connections.computeIfAbsent(key, ignored -> {
            try {
                String location = properties.resolveCollection(serviceName, collectionFile);
                JsonStore<T> store = JsonStore.fromResource(
                        resourceLoader,
                        objectMapper,
                        location,
                        idExtractor,
                        properties.isWritable(),
                        elementType);
                return new JsonMockDatastoreConnection<>(key, store);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to connect to mock datastore collection: " + key, ex);
            }
        });
    }

    @Override
    public void resetAll() {
        connections.values().forEach(MockDatastoreConnection::resetOverlay);
    }
}
