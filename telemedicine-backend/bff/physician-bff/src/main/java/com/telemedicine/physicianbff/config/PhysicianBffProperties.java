package com.telemedicine.physicianbff.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.bff")
public class PhysicianBffProperties {

    private final Downstream downstream = new Downstream();
    private final RestClient restClient = new RestClient();

    public Downstream getDownstream() {
        return downstream;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public static class Downstream {
        private String integrationService = "lb://integration-service";
        private String waitingRoomService = "lb://waiting-room-service";
        private String videoService = "lb://video-service";
        private String telemetryService = "lb://telemetry-service";
        private String monitoringService = "lb://monitoring-service";
        private String clinicalDocsService = "lb://clinical-docs-service";

        public String getIntegrationService() {
            return integrationService;
        }

        public void setIntegrationService(String integrationService) {
            this.integrationService = integrationService;
        }

        public String getWaitingRoomService() {
            return waitingRoomService;
        }

        public void setWaitingRoomService(String waitingRoomService) {
            this.waitingRoomService = waitingRoomService;
        }

        public String getVideoService() {
            return videoService;
        }

        public void setVideoService(String videoService) {
            this.videoService = videoService;
        }

        public String getTelemetryService() {
            return telemetryService;
        }

        public void setTelemetryService(String telemetryService) {
            this.telemetryService = telemetryService;
        }

        public String getMonitoringService() {
            return monitoringService;
        }

        public void setMonitoringService(String monitoringService) {
            this.monitoringService = monitoringService;
        }

        public String getClinicalDocsService() {
            return clinicalDocsService;
        }

        public void setClinicalDocsService(String clinicalDocsService) {
            this.clinicalDocsService = clinicalDocsService;
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
