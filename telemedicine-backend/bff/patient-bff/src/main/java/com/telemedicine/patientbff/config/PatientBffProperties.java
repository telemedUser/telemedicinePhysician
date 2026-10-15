package com.telemedicine.patientbff.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.bff")
public class PatientBffProperties {

    private final Downstream downstream = new Downstream();
    private final RestClient restClient = new RestClient();

    public Downstream getDownstream() {
        return downstream;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public static class Downstream {
        private String waitingRoomService = "lb://waiting-room-service";
        private String identityService = "lb://identity-service";
        private String integrationService = "lb://integration-service";
        private String videoService = "lb://video-service";
        private String clinicalDocsService = "lb://clinical-docs-service";
        private String surveyService = "lb://survey-service";

        public String getWaitingRoomService() {
            return waitingRoomService;
        }

        public void setWaitingRoomService(String waitingRoomService) {
            this.waitingRoomService = waitingRoomService;
        }

        public String getIdentityService() {
            return identityService;
        }

        public void setIdentityService(String identityService) {
            this.identityService = identityService;
        }

        public String getIntegrationService() {
            return integrationService;
        }

        public void setIntegrationService(String integrationService) {
            this.integrationService = integrationService;
        }

        public String getVideoService() {
            return videoService;
        }

        public void setVideoService(String videoService) {
            this.videoService = videoService;
        }

        public String getClinicalDocsService() {
            return clinicalDocsService;
        }

        public void setClinicalDocsService(String clinicalDocsService) {
            this.clinicalDocsService = clinicalDocsService;
        }

        public String getSurveyService() {
            return surveyService;
        }

        public void setSurveyService(String surveyService) {
            this.surveyService = surveyService;
        }
    }

    public static class RestClient {
        private Duration connectTimeout = Duration.ofMillis(500);
        private Duration readTimeout = Duration.ofMillis(2000);

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
    }
}
