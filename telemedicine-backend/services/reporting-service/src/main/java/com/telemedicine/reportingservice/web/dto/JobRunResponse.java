package com.telemedicine.reportingservice.web.dto;

public record JobRunResponse(String jobName, int processed, String status) {}
