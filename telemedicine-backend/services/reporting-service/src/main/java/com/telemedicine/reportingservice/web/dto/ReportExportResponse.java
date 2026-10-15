package com.telemedicine.reportingservice.web.dto;

import java.time.Instant;

public record ReportExportResponse(
        String exportId, String format, String downloadUrl, Instant expiresAt) {}
