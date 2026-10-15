package com.telemedicine.common.integration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** Configuration for external integrations, provider selection and HTTP clients. */
@ConfigurationProperties(prefix = "telemedicine.integration")
public class TelemedicineIntegrationProperties {

    private boolean enabled = true;

    private String configServiceBaseUrl = "http://config-service:8112";

    private Duration configCacheTtl = Duration.ofMinutes(1);

    private String safeDefaultProvider = "simulator";

    @NestedConfigurationProperty private final Simulator simulator = new Simulator();

    @NestedConfigurationProperty private final Http http = new Http();

    @NestedConfigurationProperty private final Webhook webhook = new Webhook();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigServiceBaseUrl() {
        return configServiceBaseUrl;
    }

    public void setConfigServiceBaseUrl(String configServiceBaseUrl) {
        this.configServiceBaseUrl = configServiceBaseUrl;
    }

    public Duration getConfigCacheTtl() {
        return configCacheTtl;
    }

    public void setConfigCacheTtl(Duration configCacheTtl) {
        this.configCacheTtl = configCacheTtl;
    }

    public String getSafeDefaultProvider() {
        return safeDefaultProvider;
    }

    public void setSafeDefaultProvider(String safeDefaultProvider) {
        this.safeDefaultProvider = safeDefaultProvider;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public Http getHttp() {
        return http;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public static class Simulator {

        /** Base URL for Phase 0 external simulators, Consul-aware by default. */
        private String baseUrl = "lb://external-simulators";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Http {

        private Duration connectTimeout = Duration.ofSeconds(5);

        private Duration readTimeout = Duration.ofSeconds(30);

        private boolean logRequests = true;

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public boolean isLogRequests() {
            return logRequests;
        }

        public void setLogRequests(boolean logRequests) {
            this.logRequests = logRequests;
        }
    }

    public static class Webhook {

        private Duration timestampWindow = Duration.ofMinutes(5);

        private Duration replayCacheTtl = Duration.ofHours(1);

        public Duration getTimestampWindow() {
            return timestampWindow;
        }

        public void setTimestampWindow(Duration timestampWindow) {
            this.timestampWindow = timestampWindow;
        }

        public Duration getReplayCacheTtl() {
            return replayCacheTtl;
        }

        public void setReplayCacheTtl(Duration replayCacheTtl) {
            this.replayCacheTtl = replayCacheTtl;
        }
    }
}
