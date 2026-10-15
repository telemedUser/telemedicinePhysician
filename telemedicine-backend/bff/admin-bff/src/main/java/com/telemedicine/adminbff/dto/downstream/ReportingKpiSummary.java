package com.telemedicine.adminbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportingKpiSummary(Map<String, Object> families) {}
