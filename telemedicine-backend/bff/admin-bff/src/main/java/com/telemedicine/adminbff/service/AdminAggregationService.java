package com.telemedicine.adminbff.service;

import com.telemedicine.adminbff.client.DownstreamClient;
import com.telemedicine.adminbff.dto.AuditQueryResponse;
import com.telemedicine.adminbff.dto.OperationalSnapshot;
import com.telemedicine.adminbff.dto.SchedulerJobsResponse;
import com.telemedicine.adminbff.dto.downstream.MonitoringSnapshotSummary;
import com.telemedicine.adminbff.dto.downstream.ReportingKpiSummary;
import com.telemedicine.common.audit.AuditEvent;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AdminAggregationService {

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<List<AuditEvent>>>
            AUDIT_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<List<Map<String, Object>>>>
            JOBS_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<MonitoringSnapshotSummary>>
            SNAPSHOT_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<ReportingKpiSummary>>
            KPI_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<Map<String, Object>>>
            CONFIG_PUT_TYPE = new ParameterizedTypeReference<>() {};

    private final DownstreamClient downstreamClient;
    private final RestClient configRestClient;
    private final RestClient auditRestClient;
    private final RestClient schedulerRestClient;
    private final RestClient monitoringRestClient;
    private final RestClient reportingRestClient;
    private final PlatformConfigClient platformConfigClient;

    public AdminAggregationService(
            DownstreamClient downstreamClient,
            @Qualifier("configRestClient") RestClient configRestClient,
            @Qualifier("auditRestClient") RestClient auditRestClient,
            @Qualifier("schedulerRestClient") RestClient schedulerRestClient,
            @Qualifier("monitoringRestClient") RestClient monitoringRestClient,
            @Qualifier("reportingRestClient") RestClient reportingRestClient,
            PlatformConfigClient platformConfigClient) {
        this.downstreamClient = downstreamClient;
        this.configRestClient = configRestClient;
        this.auditRestClient = auditRestClient;
        this.schedulerRestClient = schedulerRestClient;
        this.monitoringRestClient = monitoringRestClient;
        this.reportingRestClient = reportingRestClient;
        this.platformConfigClient = platformConfigClient;
    }

    public Map<String, Object> updateConfig(String key, Object value) {
        Map<String, Object> body = Map.of(key, value);
        Map<String, Object> updated =
                downstreamClient.put(
                        configRestClient, "/api/v1/config", body, CONFIG_PUT_TYPE);
        platformConfigClient.invalidate();
        return updated != null ? updated : body;
    }

    public AuditQueryResponse recentAudit() {
        List<AuditEvent> events =
                downstreamClient.getOrEmpty(auditRestClient, "/api/v1/audit", AUDIT_TYPE);
        return new AuditQueryResponse(events != null ? events : List.of());
    }

    public SchedulerJobsResponse schedulerJobs() {
        List<Map<String, Object>> jobs =
                downstreamClient.getOrEmpty(
                        schedulerRestClient, "/api/v1/scheduler/jobs", JOBS_TYPE);
        return new SchedulerJobsResponse(jobs != null ? jobs : List.of());
    }

    public OperationalSnapshot snapshot() {
        MonitoringSnapshotSummary monitoring =
                downstreamClient.getOrEmpty(
                        monitoringRestClient, "/api/v1/monitoring/snapshot", SNAPSHOT_TYPE);
        ReportingKpiSummary reporting =
                downstreamClient.getOrEmpty(
                        reportingRestClient, "/api/v1/reporting/kpis", KPI_TYPE);

        int activeSessions = monitoring != null ? monitoring.activeSessions() : 0;
        int waitingRoomQueue = monitoring != null ? monitoring.waitingRoomQueue() : 0;
        double errorRate = monitoring != null ? monitoring.errorRate() : 0.0;
        String kpiStatus = reporting != null ? "OK" : "DEGRADED";
        Map<String, Object> kpis = reporting != null ? reporting.families() : null;

        return new OperationalSnapshot(
                activeSessions, waitingRoomQueue, errorRate, kpiStatus, kpis);
    }
}
