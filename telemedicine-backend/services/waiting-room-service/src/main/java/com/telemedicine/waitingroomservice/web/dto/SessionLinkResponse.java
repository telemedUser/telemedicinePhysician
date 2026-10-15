package com.telemedicine.waitingroomservice.web.dto;

public record SessionLinkResponse(
        String appointmentId, String linkToken, String url, String event) {}
