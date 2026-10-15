package com.telemedicine.surveyservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.survey")
public class SurveyServiceProperties {

    private String defaultChannel = "email";
    private Duration dispatchBackupMinutes = Duration.ofMinutes(15);

    public String getDefaultChannel() {
        return defaultChannel;
    }

    public void setDefaultChannel(String defaultChannel) {
        this.defaultChannel = defaultChannel;
    }

    public Duration getDispatchBackupMinutes() {
        return dispatchBackupMinutes;
    }

    public void setDispatchBackupMinutes(Duration dispatchBackupMinutes) {
        this.dispatchBackupMinutes = dispatchBackupMinutes;
    }
}
