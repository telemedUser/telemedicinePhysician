package com.telemedicine.configservice.dto;

import java.time.Instant;

public record ConfigEntryDto(String key, String value, Instant updatedAt) {}
