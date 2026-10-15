package com.telemedicine.common.integration.autoconfigure;

import com.telemedicine.common.integration.ExternalSystems;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Applies resilience4j named instances for every external system when the consumer has not set
 * explicit values.
 */
public class IntegrationResilienceEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE = "telemedicineIntegrationResilienceDefaults";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        applySharedCircuitBreakerDefaults(environment, defaults);
        applySharedRetryDefaults(environment, defaults);
        applySharedBulkheadDefaults(environment, defaults);
        applySharedTimeLimiterDefaults(environment, defaults);
        applySharedRateLimiterDefaults(environment, defaults);
        for (String system : ExternalSystems.ALL) {
            applyInstanceDefaults(environment, defaults, system);
        }
        if (!defaults.isEmpty()) {
            environment
                    .getPropertySources()
                    .addLast(new MapPropertySource(PROPERTY_SOURCE, defaults));
        }
    }

    private static void applySharedCircuitBreakerDefaults(
            ConfigurableEnvironment environment, Map<String, Object> defaults) {
        putIfMissing(
                environment,
                defaults,
                "resilience4j.circuitbreaker.configs.telemedicine-external.slidingWindowSize",
                "20");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.circuitbreaker.configs.telemedicine-external.failureRateThreshold",
                "50");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.circuitbreaker.configs.telemedicine-external.waitDurationInOpenState",
                "30s");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.circuitbreaker.configs.telemedicine-external.permittedNumberOfCallsInHalfOpenState",
                "3");
    }

    private static void applySharedRetryDefaults(
            ConfigurableEnvironment environment, Map<String, Object> defaults) {
        putIfMissing(
                environment,
                defaults,
                "resilience4j.retry.configs.telemedicine-external.maxAttempts",
                "3");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.retry.configs.telemedicine-external.waitDuration",
                "200ms");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.retry.configs.telemedicine-external.enableExponentialBackoff",
                "true");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.retry.configs.telemedicine-external.exponentialBackoffMultiplier",
                "2");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.retry.configs.telemedicine-external.enableRandomizedWait",
                "true");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.retry.configs.telemedicine-external.randomizedWaitFactor",
                "0.5");
    }

    private static void applySharedBulkheadDefaults(
            ConfigurableEnvironment environment, Map<String, Object> defaults) {
        putIfMissing(
                environment,
                defaults,
                "resilience4j.bulkhead.configs.telemedicine-external.maxConcurrentCalls",
                "25");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.bulkhead.configs.telemedicine-external.maxWaitDuration",
                "0");
    }

    private static void applySharedTimeLimiterDefaults(
            ConfigurableEnvironment environment, Map<String, Object> defaults) {
        putIfMissing(
                environment,
                defaults,
                "resilience4j.timelimiter.configs.telemedicine-external.timeoutDuration",
                "30s");
    }

    private static void applySharedRateLimiterDefaults(
            ConfigurableEnvironment environment, Map<String, Object> defaults) {
        putIfMissing(
                environment,
                defaults,
                "resilience4j.ratelimiter.configs.telemedicine-external.limitForPeriod",
                "50");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.ratelimiter.configs.telemedicine-external.limitRefreshPeriod",
                "1s");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.ratelimiter.configs.telemedicine-external.timeoutDuration",
                "0");
    }

    private static void applyInstanceDefaults(
            ConfigurableEnvironment environment, Map<String, Object> defaults, String system) {
        putIfMissing(
                environment,
                defaults,
                "resilience4j.circuitbreaker.instances." + system + ".baseConfig",
                "telemedicine-external");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.retry.instances." + system + ".baseConfig",
                "telemedicine-external");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.bulkhead.instances." + system + ".baseConfig",
                "telemedicine-external");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.timelimiter.instances." + system + ".baseConfig",
                "telemedicine-external");
        putIfMissing(
                environment,
                defaults,
                "resilience4j.ratelimiter.instances." + system + ".baseConfig",
                "telemedicine-external");
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
