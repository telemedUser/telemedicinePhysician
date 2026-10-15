package com.telemedicine.waitingroomservice.web.dto;

public record StatusUpdateResponse(String appointmentId, String status, String event) {}
