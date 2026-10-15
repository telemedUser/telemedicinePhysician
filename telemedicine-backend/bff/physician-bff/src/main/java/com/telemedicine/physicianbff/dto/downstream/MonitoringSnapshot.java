package com.telemedicine.physicianbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonitoringSnapshot(int activeSessions, int waitingRoomQueue, double errorRate) {}
