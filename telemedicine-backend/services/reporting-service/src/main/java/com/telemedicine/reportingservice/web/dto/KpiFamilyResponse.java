package com.telemedicine.reportingservice.web.dto;

import java.util.Map;

public record KpiFamilyResponse(String family, Map<String, Object> metrics) {}
