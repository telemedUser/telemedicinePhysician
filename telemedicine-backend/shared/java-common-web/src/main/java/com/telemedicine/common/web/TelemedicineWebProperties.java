package com.telemedicine.common.web;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the shared web layer, all under {@code telemedicine.web}. */
@ConfigurationProperties(prefix = "telemedicine.web")
public class TelemedicineWebProperties {

    /** Master switch for every bean contributed by this module. */
    private boolean enabled = true;

    /**
     * Include the exception class and message of unexpected faults in the problem document. Must
     * stay false outside local development: 500s would otherwise leak internals to callers.
     */
    private boolean exposeInternalErrors = false;

    private final Pagination pagination = new Pagination();
    private final Idempotency idempotency = new Idempotency();
    private final RequestLogging requestLogging = new RequestLogging();
    private final Etag etag = new Etag();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isExposeInternalErrors() {
        return exposeInternalErrors;
    }

    public void setExposeInternalErrors(boolean exposeInternalErrors) {
        this.exposeInternalErrors = exposeInternalErrors;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public RequestLogging getRequestLogging() {
        return requestLogging;
    }

    public Etag getEtag() {
        return etag;
    }

    /** Bounds on page requests, applied before any query reaches the database. */
    public static class Pagination {

        /** Page size used when the caller does not ask for one. */
        private int defaultSize = 20;

        /** Hard ceiling. A caller asking for more is rejected rather than silently clamped. */
        private int maxSize = 100;

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }

    /** Replay protection for unsafe operations. Consumed from Phase 3 onwards. */
    public static class Idempotency {

        private boolean enabled = false;

        /** Ant patterns the filter applies to. Empty means every request. */
        private List<String> pathPatterns = new ArrayList<>(List.of("/v1/**"));

        /** HTTP methods considered unsafe and therefore subject to the filter. */
        private List<String> methods = new ArrayList<>(List.of("POST", "PATCH"));

        /** Reject a matching request that arrives without a key. */
        private boolean required = false;

        /** How long a completed response stays replayable. */
        private Duration ttl = Duration.ofHours(24);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getPathPatterns() {
            return pathPatterns;
        }

        public void setPathPatterns(List<String> pathPatterns) {
            this.pathPatterns = pathPatterns;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    /** Access logging. Sampled because the consultation path is latency-sensitive. */
    public static class RequestLogging {

        private boolean enabled = true;

        /** Fraction of successful requests to log, from 0.0 to 1.0. Failures always log. */
        private double sampleRate = 1.0;

        /** Paths never logged, to keep health and metrics polling out of the log volume. */
        private List<String> excludePatterns =
                new ArrayList<>(List.of("/actuator/**", "/favicon.ico"));

        /** Requests slower than this are logged at WARN regardless of the sample rate. */
        private Duration slowRequestThreshold = Duration.ofMillis(300);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(double sampleRate) {
            this.sampleRate = sampleRate;
        }

        public List<String> getExcludePatterns() {
            return excludePatterns;
        }

        public void setExcludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
        }

        public Duration getSlowRequestThreshold() {
            return slowRequestThreshold;
        }

        public void setSlowRequestThreshold(Duration slowRequestThreshold) {
            this.slowRequestThreshold = slowRequestThreshold;
        }
    }

    /** Shallow ETag support, so unchanged document and config reads can return 304. */
    public static class Etag {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
