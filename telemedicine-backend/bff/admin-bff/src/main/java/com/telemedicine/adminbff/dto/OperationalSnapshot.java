package com.telemedicine.adminbff.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OperationalSnapshot(
        int activeSessions,
        int waitingRoomQueue,
        double errorRate,
        String kpiStatus,
        Map<String, Object> kpis) {}
