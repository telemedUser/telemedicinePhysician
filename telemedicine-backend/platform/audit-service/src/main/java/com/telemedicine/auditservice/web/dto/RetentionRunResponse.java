package com.telemedicine.auditservice.web.dto;

public record RetentionRunResponse(
        int consentRecordsArchived, int consentRecordsTombstoned, long auditEventsRetained) {}
