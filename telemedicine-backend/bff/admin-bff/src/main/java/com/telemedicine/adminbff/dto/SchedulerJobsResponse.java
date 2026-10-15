package com.telemedicine.adminbff.dto;

import java.util.List;
import java.util.Map;

public record SchedulerJobsResponse(List<Map<String, Object>> jobs) {}
