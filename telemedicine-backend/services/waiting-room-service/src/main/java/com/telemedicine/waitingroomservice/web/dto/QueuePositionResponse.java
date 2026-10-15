package com.telemedicine.waitingroomservice.web.dto;

public record QueuePositionResponse(
        String appointmentId, int position, int estimatedWaitSec, String event) {}
