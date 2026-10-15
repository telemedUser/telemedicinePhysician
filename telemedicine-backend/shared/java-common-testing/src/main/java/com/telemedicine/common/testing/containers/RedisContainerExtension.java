package com.telemedicine.common.testing.containers;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Starts the shared Redis container before integration tests. */
public final class RedisContainerExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        TelemedicineContainers.startRedis();
    }
}
