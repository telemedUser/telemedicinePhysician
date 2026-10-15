package com.telemedicine.common.testing.containers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainers shared across extensions so one stack backs an entire test class.
 *
 * <p>Containers are started lazily on first use and may be reused across the JVM when {@code
 * testcontainers.reuse.enable=true}.
 */
public final class TelemedicineContainers {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:16-alpine");

    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1");

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    private static final DockerImageName MINIO_IMAGE =
            DockerImageName.parse("minio/minio:RELEASE.2024-08-17T00-00-00Z");

    private static final DockerImageName KEYCLOAK_IMAGE =
            DockerImageName.parse("quay.io/keycloak/keycloak:24.0.5");

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE).withReuse(true);

    private static final KafkaContainer KAFKA =
            new KafkaContainer(KAFKA_IMAGE).withReuse(true);

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379).withReuse(true);

    private static final MinIOContainer MINIO =
            new MinIOContainer(MINIO_IMAGE).withReuse(true);

    private static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer(KEYCLOAK_IMAGE.asCanonicalNameString())
                    .withRealmImportFile(RealmImportPaths.realmTelemedicine())
                    .withReuse(true);

    private TelemedicineContainers() {}

    public static PostgreSQLContainer<?> postgres() {
        return POSTGRES;
    }

    public static KafkaContainer kafka() {
        return KAFKA;
    }

    public static GenericContainer<?> redis() {
        return REDIS;
    }

    public static MinIOContainer minio() {
        return MINIO;
    }

    public static KeycloakContainer keycloak() {
        return KEYCLOAK;
    }

    static void startPostgres() {
        startIfNeeded(POSTGRES);
    }

    static void startKafka() {
        startIfNeeded(KAFKA);
    }

    static void startRedis() {
        startIfNeeded(REDIS);
    }

    static void startMinio() {
        startIfNeeded(MINIO);
    }

    static void startKeycloak() {
        startIfNeeded(KEYCLOAK);
    }

    private static void startIfNeeded(org.testcontainers.containers.Container<?> container) {
        if (!container.isRunning() && container instanceof Startable startable) {
            startable.start();
        }
    }
}
