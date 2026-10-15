package com.telemedicine.monitoringservice.service;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.ConnectionQualityDegraded;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.monitoringservice.config.MonitoringServiceProperties;
import com.telemedicine.monitoringservice.domain.AlertEntity;
import com.telemedicine.monitoringservice.domain.AlertRepository;
import com.telemedicine.monitoringservice.domain.AlertRuleEntity;
import com.telemedicine.monitoringservice.domain.AlertRuleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final OutboxRecorder outboxRecorder;
    private final MonitoringServiceProperties properties;
    private final RestClient notificationRestClient;

    public AlertService(
            AlertRuleRepository alertRuleRepository,
            AlertRepository alertRepository,
            OutboxRecorder outboxRecorder,
            MonitoringServiceProperties properties) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertRepository = alertRepository;
        this.outboxRecorder = outboxRecorder;
        this.properties = properties;
        this.notificationRestClient = RestClient.builder().baseUrl(properties.getNotificationServiceBaseUrl()).build();
    }

    @Transactional
    public void evaluateQuality(
            String sessionId, String appointmentId, String participantRole, int qualityScore) {
        List<AlertRuleEntity> rules = alertRuleRepository.findByEnabledTrue();
        for (AlertRuleEntity rule : rules) {
            if (!"quality_score".equals(rule.getMetric())) {
                continue;
            }
            if (matches(rule, qualityScore)) {
                openAlert(rule, sessionId, appointmentId, "Connection quality degraded to " + qualityScore);
                publishDegradedEvent(sessionId, appointmentId, participantRole, qualityScore);
            }
        }
    }

    @Transactional
    public void evaluateErrorRate(double errorRate) {
        for (AlertRuleEntity rule : alertRuleRepository.findByEnabledTrue()) {
            if (!"error_rate".equals(rule.getMetric())) {
                continue;
            }
            if (matches(rule, errorRate)) {
                openAlert(rule, null, null, "Error rate elevated to " + errorRate);
            }
        }
    }

    public long openAlertCount() {
        return alertRepository.countByStatus("OPEN");
    }

    private void openAlert(
            AlertRuleEntity rule, String sessionId, String appointmentId, String message) {
        AlertEntity alert = new AlertEntity();
        alert.setId(IdGenerator.newUlid());
        alert.setRuleId(rule.getId());
        alert.setSessionId(sessionId);
        alert.setAppointmentId(appointmentId);
        alert.setMessage(message);
        alert.setSeverity(rule.getSeverity());
        alert.setStatus("OPEN");
        alert.setOpenedAt(Instant.now());
        alert.setCreatedAt(Instant.now());
        alertRepository.save(alert);
        dispatchNotification(appointmentId, message);
    }

    private void publishDegradedEvent(
            String sessionId, String appointmentId, String participantRole, int qualityScore) {
        ConnectionQualityDegraded payload =
                new ConnectionQualityDegraded(
                        appointmentId, sessionId, participantRole, qualityScore, Instant.now());
        outboxRecorder.record("session-health", sessionId, payload, IdGenerator.correlationId());
    }

    private void dispatchNotification(String appointmentId, String message) {
        try {
            notificationRestClient
                    .post()
                    .uri("/api/v1/notifications/send")
                    .body(
                            Map.of(
                                    "templateKey",
                                    "monitoring-alert",
                                    "channel",
                                    "email",
                                    "recipientRef",
                                    "ops-team",
                                    "appointmentId",
                                    appointmentId != null ? appointmentId : "platform",
                                    "variables",
                                    Map.of("message", message)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("Alert notification dispatch failed open: {}", exception.getMessage());
        }
    }

    private static boolean matches(AlertRuleEntity rule, double observed) {
        return switch (rule.getComparison()) {
            case "LT" -> observed < rule.getThreshold();
            case "GT" -> observed > rule.getThreshold();
            default -> false;
        };
    }
}
