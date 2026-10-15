package com.telemedicine.common.events;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for Kafka publishing and consumption. */
@ConfigurationProperties(prefix = "telemedicine.events")
public class TelemedicineEventsProperties {

    private boolean enabled = true;

    /** Defaults to the Phase 0 compose value. */
    private String bootstrapServers = "kafka:9092";

    /** CloudEvents {@code ce_source}, normally the service name. */
    private String source = "${spring.application.name:telemedicine}";

    private final Producer producer = new Producer();
    private final Consumer consumer = new Consumer();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Producer getProducer() {
        return producer;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public static class Producer {

        private String acks = "all";
        private int retries = 3;
        private int maxInFlight = 5;
        private String compressionType = "zstd";
        private Duration deliveryTimeout = Duration.ofMinutes(2);

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

        public int getMaxInFlight() {
            return maxInFlight;
        }

        public void setMaxInFlight(int maxInFlight) {
            this.maxInFlight = maxInFlight;
        }

        public String getCompressionType() {
            return compressionType;
        }

        public void setCompressionType(String compressionType) {
            this.compressionType = compressionType;
        }

        public Duration getDeliveryTimeout() {
            return deliveryTimeout;
        }

        public void setDeliveryTimeout(Duration deliveryTimeout) {
            this.deliveryTimeout = deliveryTimeout;
        }
    }

    public static class Consumer {

        private int concurrency = 1;
        private int maxRetries = 3;
        private Duration initialRetryInterval = Duration.ofSeconds(1);
        private Duration maxRetryInterval = Duration.ofSeconds(30);
        private double retryMultiplier = 2.0;
        private Duration processedEventTtl = Duration.ofDays(7);

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Duration getInitialRetryInterval() {
            return initialRetryInterval;
        }

        public void setInitialRetryInterval(Duration initialRetryInterval) {
            this.initialRetryInterval = initialRetryInterval;
        }

        public Duration getMaxRetryInterval() {
            return maxRetryInterval;
        }

        public void setMaxRetryInterval(Duration maxRetryInterval) {
            this.maxRetryInterval = maxRetryInterval;
        }

        public double getRetryMultiplier() {
            return retryMultiplier;
        }

        public void setRetryMultiplier(double retryMultiplier) {
            this.retryMultiplier = retryMultiplier;
        }

        public Duration getProcessedEventTtl() {
            return processedEventTtl;
        }

        public void setProcessedEventTtl(Duration processedEventTtl) {
            this.processedEventTtl = processedEventTtl;
        }
    }
}
