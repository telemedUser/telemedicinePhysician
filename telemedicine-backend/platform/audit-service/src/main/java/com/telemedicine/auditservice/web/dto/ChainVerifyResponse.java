package com.telemedicine.auditservice.web.dto;

public record ChainVerifyResponse(
        boolean valid, long eventsChecked, String lastHash, long lastSequence, String message) {}
