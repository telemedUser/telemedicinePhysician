package com.telemedicine.monitoringservice.service;

import com.telemedicine.monitoringservice.config.MonitoringServiceProperties;
import com.telemedicine.monitoringservice.web.dto.VideoQualityRequest;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QualityService {

    private final SessionHealthService sessionHealthService;
    private final AlertService alertService;
    private final MonitoringSnapshotService snapshotService;
    private final MonitoringServiceProperties properties;

    public QualityService(
            SessionHealthService sessionHealthService,
            AlertService alertService,
            MonitoringSnapshotService snapshotService,
            MonitoringServiceProperties properties) {
        this.sessionHealthService = sessionHealthService;
        this.alertService = alertService;
        this.snapshotService = snapshotService;
        this.properties = properties;
    }

    @Transactional
    public void recordQuality(VideoQualityRequest request) {
        Instant now = Instant.now();
        sessionHealthService.recordQuality(request.sessionId(), request.qualityScore(), now);
        if (request.qualityScore() < properties.getQualityDegradedThreshold()) {
            alertService.evaluateQuality(
                    request.sessionId(),
                    request.appointmentId(),
                    request.participantRole() != null ? request.participantRole() : "UNKNOWN",
                    request.qualityScore());
        }
        snapshotService.refreshSnapshot();
    }
}
