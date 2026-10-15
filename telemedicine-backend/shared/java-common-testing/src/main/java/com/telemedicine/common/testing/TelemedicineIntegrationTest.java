package com.telemedicine.common.testing;

import com.telemedicine.common.testing.containers.KafkaContainerExtension;
import com.telemedicine.common.testing.containers.KeycloakContainerExtension;
import com.telemedicine.common.testing.containers.MinioContainerExtension;
import com.telemedicine.common.testing.containers.PostgresContainerExtension;
import com.telemedicine.common.testing.containers.RedisContainerExtension;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Composed annotation for full-stack integration tests backed by the Phase 0 infrastructure
 * containers.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith({
    PostgresContainerExtension.class,
    KafkaContainerExtension.class,
    RedisContainerExtension.class,
    MinioContainerExtension.class,
    KeycloakContainerExtension.class
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public @interface TelemedicineIntegrationTest {}
