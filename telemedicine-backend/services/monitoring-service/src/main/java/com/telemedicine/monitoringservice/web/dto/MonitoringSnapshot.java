package com.telemedicine.monitoringservice.web.dto;

public record MonitoringSnapshot(
        int activeSessions,
        int waitingRoomQueue,
        double errorRate,
        Double connectionQuality,
        Integer openAlerts,
        Double sloBurnRate) {}
