package com.telemedicine.common.persistence;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for shared persistence defaults. */
@ConfigurationProperties(prefix = "telemedicine.persistence")
public class TelemedicinePersistenceProperties {

    private boolean enabled = true;

    private final Datasource datasource = new Datasource();

    private final Jpa jpa = new Jpa();

    private final PhiEncryption phiEncryption = new PhiEncryption();

    private final Cache cache = new Cache();

    private final Idempotency idempotency = new Idempotency();

    private final ProcessedEvents processedEvents = new ProcessedEvents();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public Jpa getJpa() {
        return jpa;
    }

    public PhiEncryption getPhiEncryption() {
        return phiEncryption;
    }

    public Cache getCache() {
        return cache;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public ProcessedEvents getProcessedEvents() {
        return processedEvents;
    }

    public static class Datasource {

        private int maximumPoolSize = 10;

        private Duration connectionTimeout = Duration.ofSeconds(30);

        private Duration leakDetectionThreshold = Duration.ofMinutes(2);

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Duration getLeakDetectionThreshold() {
            return leakDetectionThreshold;
        }

        public void setLeakDetectionThreshold(Duration leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
        }
    }

    public static class Jpa {

        private boolean openInView = false;

        public boolean isOpenInView() {
            return openInView;
        }

        public void setOpenInView(boolean openInView) {
            this.openInView = openInView;
        }
    }

    public static class PhiEncryption {

        /** Base64 AES-256 key material, or {@code keyId:base64} pairs separated by commas. */
        private String key = System.getenv("TELEMEDICINE_PHI_KEY");

        private String activeKeyId = "v1";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getActiveKeyId() {
            return activeKeyId;
        }

        public void setActiveKeyId(String activeKeyId) {
            this.activeKeyId = activeKeyId;
        }
    }

    public static class Cache {

        private boolean enabled = true;

        private Duration defaultTtl = Duration.ofMinutes(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }
    }

    public static class Idempotency {

        /** {@code jpa}, {@code redis}, or {@code auto} to prefer Redis when available. */
        private String store = "auto";

        private Duration defaultTtl = Duration.ofHours(24);

        public String getStore() {
            return store;
        }

        public void setStore(String store) {
            this.store = store;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }
    }

    public static class ProcessedEvents {

        private Duration retention = Duration.ofDays(7);

        private Duration purgeInterval = Duration.ofHours(1);

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }

        public Duration getPurgeInterval() {
            return purgeInterval;
        }

        public void setPurgeInterval(Duration purgeInterval) {
            this.purgeInterval = purgeInterval;
        }
    }
}
