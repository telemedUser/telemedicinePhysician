package com.telemedicine.patientbff.dto;

public record WaitingRoomState(
        AggregationStatus status, int position, int estimatedWaitSec, String queueStatus) {}
