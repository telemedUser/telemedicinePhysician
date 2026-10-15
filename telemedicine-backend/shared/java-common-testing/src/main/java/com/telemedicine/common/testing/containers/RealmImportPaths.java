package com.telemedicine.common.testing.containers;

import java.nio.file.Path;

/** Resolves the Phase 0 Keycloak realm JSON for Testcontainers import. */
final class RealmImportPaths {

    private static final String REALM_FILE = "realm-telemedicine.json";

    private RealmImportPaths() {}

    static String realmTelemedicine() {
        Path platform =
                Path.of(System.getProperty("telemedicine.platform.dir", ""))
                        .resolve("infra/keycloak")
                        .resolve(REALM_FILE);
        if (platform.toFile().exists()) {
            return platform.toAbsolutePath().toString();
        }
        return REALM_FILE;
    }
}
