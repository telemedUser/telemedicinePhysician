package com.telemedicine.reportingservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.reporting")
public class ReportingServiceProperties {

    private String exportBucket = "audit-exports";
    private String exportPrefix = "reporting/";
    private Duration exportUrlTtl = Duration.ofHours(1);

    public String getExportBucket() {
        return exportBucket;
    }

    public void setExportBucket(String exportBucket) {
        this.exportBucket = exportBucket;
    }

    public String getExportPrefix() {
        return exportPrefix;
    }

    public void setExportPrefix(String exportPrefix) {
        this.exportPrefix = exportPrefix;
    }

    public Duration getExportUrlTtl() {
        return exportUrlTtl;
    }

    public void setExportUrlTtl(Duration exportUrlTtl) {
        this.exportUrlTtl = exportUrlTtl;
    }
}
