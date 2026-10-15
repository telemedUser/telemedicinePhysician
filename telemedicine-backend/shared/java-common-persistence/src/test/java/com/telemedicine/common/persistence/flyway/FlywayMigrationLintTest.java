package com.telemedicine.common.persistence.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FlywayMigrationLintTest {

    private static final Pattern NAMING =
            Pattern.compile("V\\d+__[a-z0-9_]+\\.sql", Pattern.CASE_INSENSITIVE);

    @Test
    void commonMigrationsFollowNamingConvention() throws Exception {
        Path migrations =
                Path.of("src/main/resources/db/migration/common").toAbsolutePath().normalize();
        assertThat(Files.exists(migrations)).isTrue();
        try (var paths = Files.list(migrations)) {
            paths.forEach(
                    file -> {
                        String name = file.getFileName().toString();
                        assertThat(NAMING.matcher(name).matches())
                                .as("Migration file name %s", name)
                                .isTrue();
                    });
        }
    }
}
