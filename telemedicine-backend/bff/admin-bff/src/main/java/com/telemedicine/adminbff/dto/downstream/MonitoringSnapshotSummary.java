package com.telemedicine.adminbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonitoringSnapshotSummary(int activeSessions, int waitingRoomQueue, double errorRate) {}
