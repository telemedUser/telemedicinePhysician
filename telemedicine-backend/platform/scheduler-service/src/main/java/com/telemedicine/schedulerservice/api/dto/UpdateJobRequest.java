package com.telemedicine.schedulerservice.api.dto;

public record UpdateJobRequest(Boolean enabled, String cron) {}
