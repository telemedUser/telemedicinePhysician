package com.telemedicine.auditservice.web.dto;

import java.time.Instant;

public record AuditExportResponse(
        String objectKey, String downloadUrl, Instant expiresAt, long recordCount) {}
