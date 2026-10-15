package com.telemedicine.auditservice.web.dto;

import java.time.Instant;

public record AuditEventResponse(
        String auditId,
        String actorId,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        String outcome,
        Instant occurredAt,
        String actorIp,
        String userAgent,
        String reason,
        long sequence,
        String previousHash,
        String hash,
        String correlationId) {}
