package com.telemedicine.reportingservice.export;

import java.time.Duration;

public record MinioExportStorageProperties(String bucket, Duration urlTtl) {}
