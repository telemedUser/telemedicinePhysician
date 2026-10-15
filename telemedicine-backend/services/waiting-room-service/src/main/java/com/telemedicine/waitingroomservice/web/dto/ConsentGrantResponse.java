package com.telemedicine.waitingroomservice.web.dto;

public record ConsentGrantResponse(
        String appointmentId, String consentType, String version, String status) {}
