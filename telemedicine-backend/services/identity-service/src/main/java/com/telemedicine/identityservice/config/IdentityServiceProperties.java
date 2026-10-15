package com.telemedicine.identityservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.identity")
public class IdentityServiceProperties {

    private int maxAttempts = 3;
    private Duration lockoutDuration = Duration.ofMinutes(15);
    private String integrationServiceBaseUrl = "http://integration-service:8107";

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getLockoutDuration() {
        return lockoutDuration;
    }

    public void setLockoutDuration(Duration lockoutDuration) {
        this.lockoutDuration = lockoutDuration;
    }

    public String getIntegrationServiceBaseUrl() {
        return integrationServiceBaseUrl;
    }

    public void setIntegrationServiceBaseUrl(String integrationServiceBaseUrl) {
        this.integrationServiceBaseUrl = integrationServiceBaseUrl;
    }
}
