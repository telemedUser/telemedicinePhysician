package com.telemedicine.common.testing.containers;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Starts Keycloak with the Phase 0 {@code realm-telemedicine.json} so tests authenticate against
 * the same clients and roles as runtime.
 */
public final class KeycloakContainerExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        TelemedicineContainers.startKeycloak();
    }
}
