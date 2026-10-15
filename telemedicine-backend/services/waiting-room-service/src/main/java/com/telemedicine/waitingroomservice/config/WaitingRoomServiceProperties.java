package com.telemedicine.waitingroomservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.waiting-room")
public class WaitingRoomServiceProperties {

    private String platformConsentVersion = "1.0";
    private String clinicalConsentVersion = "1.0";
    private String sessionLinkSecret = "change-me-locally";
    private Duration sessionLinkTtl = Duration.ofHours(24);
    private String sessionLinkBaseUrl = "http://localhost:3000/journey";
    private int queueAvgWaitSec = 90;
    private Duration waitingRoomTimeout = Duration.ofMinutes(30);
    private Duration noShowThreshold = Duration.ofMinutes(15);
    private String integrationServiceUrl = "http://integration-service:8107";
    private String identityServiceUrl = "http://identity-service:8102";

    public String getPlatformConsentVersion() {
        return platformConsentVersion;
    }

    public void setPlatformConsentVersion(String platformConsentVersion) {
        this.platformConsentVersion = platformConsentVersion;
    }

    public String getClinicalConsentVersion() {
        return clinicalConsentVersion;
    }

    public void setClinicalConsentVersion(String clinicalConsentVersion) {
        this.clinicalConsentVersion = clinicalConsentVersion;
    }

    public String getSessionLinkSecret() {
        return sessionLinkSecret;
    }

    public void setSessionLinkSecret(String sessionLinkSecret) {
        this.sessionLinkSecret = sessionLinkSecret;
    }

    public Duration getSessionLinkTtl() {
        return sessionLinkTtl;
    }

    public void setSessionLinkTtl(Duration sessionLinkTtl) {
        this.sessionLinkTtl = sessionLinkTtl;
    }

    public String getSessionLinkBaseUrl() {
        return sessionLinkBaseUrl;
    }

    public void setSessionLinkBaseUrl(String sessionLinkBaseUrl) {
        this.sessionLinkBaseUrl = sessionLinkBaseUrl;
    }

    public int getQueueAvgWaitSec() {
        return queueAvgWaitSec;
    }

    public void setQueueAvgWaitSec(int queueAvgWaitSec) {
        this.queueAvgWaitSec = queueAvgWaitSec;
    }

    public Duration getWaitingRoomTimeout() {
        return waitingRoomTimeout;
    }

    public void setWaitingRoomTimeout(Duration waitingRoomTimeout) {
        this.waitingRoomTimeout = waitingRoomTimeout;
    }

    public Duration getNoShowThreshold() {
        return noShowThreshold;
    }

    public void setNoShowThreshold(Duration noShowThreshold) {
        this.noShowThreshold = noShowThreshold;
    }

    public String getIntegrationServiceUrl() {
        return integrationServiceUrl;
    }

    public void setIntegrationServiceUrl(String integrationServiceUrl) {
        this.integrationServiceUrl = integrationServiceUrl;
    }

    public String getIdentityServiceUrl() {
        return identityServiceUrl;
    }

    public void setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
    }
}
