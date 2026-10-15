package com.telemedicine.monitoringservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.monitoring")
public class MonitoringServiceProperties {

    private int qualityDegradedThreshold = 50;
    private String snapshotCacheKey = "monitoring:snapshot";
    private Duration snapshotCacheTtl = Duration.ofSeconds(30);
    private int sloWindowMinutes = 60;
    private String notificationServiceBaseUrl = "http://notification-service:8106";

    public int getQualityDegradedThreshold() {
        return qualityDegradedThreshold;
    }

    public void setQualityDegradedThreshold(int qualityDegradedThreshold) {
        this.qualityDegradedThreshold = qualityDegradedThreshold;
    }

    public String getSnapshotCacheKey() {
        return snapshotCacheKey;
    }

    public void setSnapshotCacheKey(String snapshotCacheKey) {
        this.snapshotCacheKey = snapshotCacheKey;
    }

    public Duration getSnapshotCacheTtl() {
        return snapshotCacheTtl;
    }

    public void setSnapshotCacheTtl(Duration snapshotCacheTtl) {
        this.snapshotCacheTtl = snapshotCacheTtl;
    }

    public int getSloWindowMinutes() {
        return sloWindowMinutes;
    }

    public void setSloWindowMinutes(int sloWindowMinutes) {
        this.sloWindowMinutes = sloWindowMinutes;
    }

    public String getNotificationServiceBaseUrl() {
        return notificationServiceBaseUrl;
    }

    public void setNotificationServiceBaseUrl(String notificationServiceBaseUrl) {
        this.notificationServiceBaseUrl = notificationServiceBaseUrl;
    }
}
