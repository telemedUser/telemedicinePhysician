package com.telemedicine.common.observability.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Applies observability defaults only when the consumer has not set explicit values — Prometheus
 * exposure, K8s probe groups and OTLP export to the Phase 0 Jaeger endpoint.
 */
public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE = "telemedicineObservabilityDefaults";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        putIfMissing(
                environment,
                defaults,
                "management.endpoints.web.exposure.include",
                "health,info,prometheus");
        putIfMissing(environment, defaults, "management.endpoint.health.probes.enabled", "true");
        putIfMissing(environment, defaults, "management.health.livenessstate.enabled", "true");
        putIfMissing(environment, defaults, "management.health.readinessstate.enabled", "true");
        putIfMissing(environment, defaults, "management.endpoint.prometheus.enabled", "true");
        putIfMissing(
                environment,
                defaults,
                "management.otlp.tracing.endpoint",
                environment.getProperty(
                        "telemedicine.observability.tracing.otlp-endpoint", "http://jaeger:4317"));
        putIfMissing(environment, defaults, "management.tracing.sampling.probability", "1.0");
        putIfMissing(
                environment,
                defaults,
                "logging.config",
                "classpath:com/telemedicine/common/observability/logback-spring.xml");
        putIfMissing(
                environment,
                defaults,
                "management.metrics.tags.application",
                "${spring.application.name}");
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
