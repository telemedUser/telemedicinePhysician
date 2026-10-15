package com.telemedicine.auditservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record AuditIngestRequest(
        String auditId,
        @NotBlank String actorId,
        String actorRole,
        @NotBlank String action,
        @NotBlank String resourceType,
        @NotBlank String resourceId,
        @NotBlank String outcome,
        Instant occurredAt,
        String actorIp,
        String userAgent,
        String reason,
        String correlationId) {}
