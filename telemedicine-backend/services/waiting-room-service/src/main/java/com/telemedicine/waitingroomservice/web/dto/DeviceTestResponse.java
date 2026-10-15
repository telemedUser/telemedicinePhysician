package com.telemedicine.waitingroomservice.web.dto;

public record DeviceTestResponse(
        String appointmentId,
        boolean camera,
        boolean microphone,
        int networkMbps,
        String event) {}
