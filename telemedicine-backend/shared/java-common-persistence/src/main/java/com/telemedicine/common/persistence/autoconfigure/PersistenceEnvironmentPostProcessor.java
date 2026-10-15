package com.telemedicine.common.persistence.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Applies HikariCP and JPA defaults when the consumer has not set explicit values. */
public class PersistenceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE = "telemedicinePersistenceDefaults";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        putIfMissing(
                environment,
                defaults,
                "spring.jpa.open-in-view",
                environment.getProperty("telemedicine.persistence.jpa.open-in-view", "false"));
        putIfMissing(
                environment,
                defaults,
                "spring.datasource.hikari.maximum-pool-size",
                environment.getProperty(
                        "telemedicine.persistence.datasource.maximum-pool-size", "10"));
        putIfMissing(
                environment,
                defaults,
                "spring.datasource.hikari.connection-timeout",
                environment.getProperty(
                        "telemedicine.persistence.datasource.connection-timeout", "30000"));
        putIfMissing(
                environment,
                defaults,
                "spring.datasource.hikari.leak-detection-threshold",
                environment.getProperty(
                        "telemedicine.persistence.datasource.leak-detection-threshold", "120000"));
        putIfMissing(
                environment,
                defaults,
                "spring.flyway.locations",
                "classpath:db/migration,classpath:db/migration/common");
        if (!defaults.isEmpty()) {
            environment
                    .getPropertySources()
                    .addLast(new MapPropertySource(PROPERTY_SOURCE, defaults));
        }
    }

    private static void putIfMissing(
            ConfigurableEnvironment environment,
            Map<String, Object> target,
            String key,
            Object value) {
        if (!environment.containsProperty(key)) {
            target.put(key, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
