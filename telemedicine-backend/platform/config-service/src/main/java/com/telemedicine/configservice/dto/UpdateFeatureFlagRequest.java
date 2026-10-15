package com.telemedicine.configservice.dto;

public record UpdateFeatureFlagRequest(boolean enabled, String payloadJson) {}
