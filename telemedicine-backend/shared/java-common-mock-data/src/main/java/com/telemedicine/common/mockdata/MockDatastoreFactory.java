package com.telemedicine.common.mockdata;

import java.util.function.Function;

/**
 * Opens mock datastore connections (like a {@code DataSource} for JSON seeds).
 */
public interface MockDatastoreFactory {

    <T> MockDatastoreConnection<T> connect(
            String serviceName,
            String collectionFile,
            Class<T> elementType,
            Function<T, String> idExtractor);

    void resetAll();
}
