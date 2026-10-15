package com.telemedicine.clinicaldocsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.minio")
public record MinioProperties(String endpoint, String accessKey, String secretKey) {}
