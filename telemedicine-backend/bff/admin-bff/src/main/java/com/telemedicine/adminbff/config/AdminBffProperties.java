package com.telemedicine.adminbff.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.bff")
public class AdminBffProperties {

    private final Downstream downstream = new Downstream();
    private final RestClient restClient = new RestClient();

    public Downstream getDownstream() {
        return downstream;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public static class Downstream {
        private String configService = "lb://config-service";
        private String auditService = "lb://audit-service";
        private String schedulerService = "lb://scheduler-service";
        private String monitoringService = "lb://monitoring-service";
        private String reportingService = "lb://reporting-service";

        public String getConfigService() {
            return configService;
        }

        public void setConfigService(String configService) {
            this.configService = configService;
        }

        public String getAuditService() {
            return auditService;
        }

        public void setAuditService(String auditService) {
            this.auditService = auditService;
        }

        public String getSchedulerService() {
            return schedulerService;
        }

        public void setSchedulerService(String schedulerService) {
            this.schedulerService = schedulerService;
        }

        public String getMonitoringService() {
            return monitoringService;
        }

        public void setMonitoringService(String monitoringService) {
            this.monitoringService = monitoringService;
        }

        public String getReportingService() {
            return reportingService;
        }

        public void setReportingService(String reportingService) {
            this.reportingService = reportingService;
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
