package com.telemedicine.common.observability;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for correlation, tracing, metrics and logging defaults. */
@ConfigurationProperties(prefix = "telemedicine.observability")
public class TelemedicineObservabilityProperties {

    private boolean enabled = true;

    private final Correlation correlation = new Correlation();

    private final Tracing tracing = new Tracing();

    private final Metrics metrics = new Metrics();

    private final Logging logging = new Logging();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Correlation getCorrelation() {
        return correlation;
    }

    public Tracing getTracing() {
        return tracing;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public Logging getLogging() {
        return logging;
    }

    public static class Correlation {

        /** Optional inbound header carrying the appointment context for journey logs. */
        private String appointmentHeader = "X-Appointment-Id";

        public String getAppointmentHeader() {
            return appointmentHeader;
        }

        public void setAppointmentHeader(String appointmentHeader) {
            this.appointmentHeader = appointmentHeader;
        }
    }

    public static class Tracing {

        private String otlpEndpoint = "http://jaeger:4317";

        private double samplingProbability = 1.0;

        public String getOtlpEndpoint() {
            return otlpEndpoint;
        }

        public void setOtlpEndpoint(String otlpEndpoint) {
            this.otlpEndpoint = otlpEndpoint;
        }

        public double getSamplingProbability() {
            return samplingProbability;
        }

        public void setSamplingProbability(double samplingProbability) {
            this.samplingProbability = samplingProbability;
        }
    }

    public static class Metrics {

        private boolean prometheusEnabled = true;

        private double[] sloMillis = {100, 200, 300, 500, 1000};

        public boolean isPrometheusEnabled() {
            return prometheusEnabled;
        }

        public void setPrometheusEnabled(boolean prometheusEnabled) {
            this.prometheusEnabled = prometheusEnabled;
        }

        public double[] getSloMillis() {
            return sloMillis.clone();
        }

        public void setSloMillis(double[] sloMillis) {
            this.sloMillis = sloMillis.clone();
        }
    }

    public static class Logging {

        private boolean jsonEnabled = false;

        private Duration phiRedactionCacheTtl = Duration.ofMinutes(5);

        public boolean isJsonEnabled() {
            return jsonEnabled;
        }

        public void setJsonEnabled(boolean jsonEnabled) {
            this.jsonEnabled = jsonEnabled;
        }

        public Duration getPhiRedactionCacheTtl() {
            return phiRedactionCacheTtl;
        }

        public void setPhiRedactionCacheTtl(Duration phiRedactionCacheTtl) {
            this.phiRedactionCacheTtl = phiRedactionCacheTtl;
        }
    }
}
