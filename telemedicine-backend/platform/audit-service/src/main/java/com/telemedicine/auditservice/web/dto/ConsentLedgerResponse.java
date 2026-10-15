package com.telemedicine.auditservice.web.dto;

import java.time.Instant;

public record ConsentLedgerResponse(
        String consentId,
        String appointmentId,
        String subjectRef,
        String consentType,
        String version,
        String scope,
        boolean granted,
        Instant recordedAt,
        Instant withdrawnAt) {}
