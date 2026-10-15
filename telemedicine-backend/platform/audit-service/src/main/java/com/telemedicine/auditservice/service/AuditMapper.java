package com.telemedicine.auditservice.service;

import com.telemedicine.auditservice.domain.AuditEventEntity;
import com.telemedicine.auditservice.domain.ConsentLedgerEntity;
import com.telemedicine.auditservice.web.dto.AuditEventResponse;
import com.telemedicine.auditservice.web.dto.ConsentLedgerResponse;

final class AuditMapper {

    private AuditMapper() {}

    static AuditEventResponse toResponse(AuditEventEntity entity) {
        return new AuditEventResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getActorRole(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getOutcome(),
                entity.getOccurredAt(),
                entity.getActorIp(),
                entity.getUserAgent(),
                entity.getReason(),
                entity.getSequence(),
                entity.getPreviousHash(),
                entity.getHash(),
                entity.getCorrelationId());
    }

    static ConsentLedgerResponse toResponse(ConsentLedgerEntity entity) {
        return new ConsentLedgerResponse(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getSubjectRef(),
                entity.getConsentType(),
                entity.getVersion(),
                entity.getScope(),
                entity.isGranted(),
                entity.getRecordedAt(),
                entity.getWithdrawnAt());
    }
}
