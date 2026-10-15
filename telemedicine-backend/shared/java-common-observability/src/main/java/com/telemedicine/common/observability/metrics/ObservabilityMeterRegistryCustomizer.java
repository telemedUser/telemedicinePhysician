package com.telemedicine.common.observability.metrics;

import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.core.env.Environment;

/** Applies common tags and SLO histogram buckets for HTTP server latency metrics. */
public class ObservabilityMeterRegistryCustomizer
        implements MeterRegistryCustomizer<MeterRegistry> {

    private final Environment environment;
    private final TelemedicineObservabilityProperties properties;

    public ObservabilityMeterRegistryCustomizer(
            Environment environment, TelemedicineObservabilityProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void customize(MeterRegistry registry) {
        String service = environment.getProperty("spring.application.name", "telemedicine-service");
        String env = environment.getProperty("spring.profiles.active", "default");
        String version = environment.getProperty("info.app.version", "unknown");

        registry.config().commonTags("service", service, "env", env, "version", version);
        registry.config().meterFilter(new SloMeterFilter(properties.getMetrics().getSloMillis()));
    }

    private static final class SloMeterFilter implements MeterFilter {

        private final double[] sloMillis;

        private SloMeterFilter(double[] sloMillis) {
            this.sloMillis = sloMillis.clone();
        }

        @Override
        public DistributionStatisticConfig configure(
                Meter.Id id, DistributionStatisticConfig config) {
            if (id.getName().startsWith("http.server.requests")
                    || id.getName().startsWith("telemedicine.journey")) {
                return DistributionStatisticConfig.builder()
                        .serviceLevelObjectives(sloMillis)
                        .build()
                        .merge(config);
            }
            return config;
        }

        @Override
        public MeterFilterReply accept(Meter.Id id) {
            return MeterFilterReply.NEUTRAL;
        }
    }
}
