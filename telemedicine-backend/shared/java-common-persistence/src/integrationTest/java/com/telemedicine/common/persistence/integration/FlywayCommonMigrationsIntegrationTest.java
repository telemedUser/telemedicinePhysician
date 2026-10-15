package com.telemedicine.common.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FlywayCommonMigrationsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("telemedicine")
                    .withUsername("telemedicine")
                    .withPassword("telemedicine");

    @Test
    void migratesSharedCommonScripts() throws Exception {
        Path common =
                Path.of("src/main/resources/db/migration/common").toAbsolutePath().normalize();
        assertThat(Files.exists(common)).isTrue();

        Flyway flyway =
                Flyway.configure()
                        .dataSource(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword())
                        .locations("filesystem:" + common)
                        .load();
        flyway.migrate();

        assertThat(
                        POSTGRES.execInContainer(
                                "psql",
                                "-U",
                                POSTGRES.getUsername(),
                                "-d",
                                POSTGRES.getDatabaseName(),
                                "-c",
                                "\\d outbox_events"))
                .asString()
                .contains("payload");
    }
}
