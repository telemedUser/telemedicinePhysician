package com.telemedicine.integrationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.integration-service")
public class IntegrationServiceProperties {

    private String waitingRoomBaseUrl = "http://waiting-room-service:8101";

    public String getWaitingRoomBaseUrl() {
        return waitingRoomBaseUrl;
    }

    public void setWaitingRoomBaseUrl(String waitingRoomBaseUrl) {
        this.waitingRoomBaseUrl = waitingRoomBaseUrl;
    }
}
