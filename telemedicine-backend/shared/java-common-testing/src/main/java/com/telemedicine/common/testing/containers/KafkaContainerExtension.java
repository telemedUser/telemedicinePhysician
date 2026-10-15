package com.telemedicine.common.testing.containers;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Starts the shared Kafka container before integration tests. */
public final class KafkaContainerExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        TelemedicineContainers.startKafka();
    }
}
