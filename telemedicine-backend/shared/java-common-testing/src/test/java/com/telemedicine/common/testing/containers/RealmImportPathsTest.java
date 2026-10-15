package com.telemedicine.common.testing.containers;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RealmImportPathsTest {

    @Test
    void resolvesPlatformRealmWhenPresent() {
        Path platform = Path.of(System.getProperty("telemedicine.platform.dir", ""));
        if (platform.resolve("infra/keycloak/realm-telemedicine.json").toFile().exists()) {
            assertThat(RealmImportPaths.realmTelemedicine()).endsWith("realm-telemedicine.json");
        } else {
            assertThat(RealmImportPaths.realmTelemedicine()).isEqualTo("realm-telemedicine.json");
        }
    }
}
