package com.telemedicine.patientbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QueuePosition(
        String appointmentId, int position, int estimatedWaitSec, String event) {}
