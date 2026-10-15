package com.telemedicine.auditservice.export;

import java.time.Duration;
import java.time.Instant;

public record MinioExportStorageProperties(String bucket, Duration urlTtl) {}
