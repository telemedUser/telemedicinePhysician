package com.telemedicine.auditservice.web.dto;

import java.time.Instant;

public record AuditExportRequest(String format, Instant from, Instant to, String actorId, String action) {}
