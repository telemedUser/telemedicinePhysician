package com.telemedicine.auditservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.audit-service")
public record AuditServiceProperties(
        int retentionDays, String exportBucket, Duration exportUrlTtl) {}
