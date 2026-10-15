package com.telemedicine.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.notification")
public class NotificationServiceProperties {

    private String defaultLocale = "es";
    private int reminderWindowHours = 24;
    private String integrationServiceBaseUrl = "http://integration-service:8107";

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public int getReminderWindowHours() {
        return reminderWindowHours;
    }

    public void setReminderWindowHours(int reminderWindowHours) {
        this.reminderWindowHours = reminderWindowHours;
    }

    public String getIntegrationServiceBaseUrl() {
        return integrationServiceBaseUrl;
    }

    public void setIntegrationServiceBaseUrl(String integrationServiceBaseUrl) {
        this.integrationServiceBaseUrl = integrationServiceBaseUrl;
    }
}
