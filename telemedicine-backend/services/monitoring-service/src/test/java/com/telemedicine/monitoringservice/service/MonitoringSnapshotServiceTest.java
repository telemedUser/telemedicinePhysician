package com.telemedicine.monitoringservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.monitoringservice.cache.SnapshotCache;
import com.telemedicine.monitoringservice.config.MonitoringServiceProperties;
import com.telemedicine.monitoringservice.domain.AlertRepository;
import com.telemedicine.monitoringservice.domain.AlertRuleEntity;
import com.telemedicine.monitoringservice.domain.AlertRuleRepository;
import com.telemedicine.monitoringservice.domain.SessionHealthEntity;
import com.telemedicine.monitoringservice.domain.SessionHealthRepository;
import com.telemedicine.monitoringservice.domain.SloWindowEntity;
import com.telemedicine.monitoringservice.domain.SloWindowRepository;
import com.telemedicine.monitoringservice.web.dto.MonitoringSnapshot;
import com.telemedicine.monitoringservice.web.dto.VideoQualityRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class MonitoringSnapshotServiceTest {

    @Mock private SessionHealthRepository sessionHealthRepository;
    @Mock private SloWindowRepository sloWindowRepository;
    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private OutboxRecorder outboxRecorder;
    @Mock private SnapshotCache snapshotCache;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private SessionHealthService sessionHealthService;
    private SloService sloService;
    private AlertService alertService;
    private MonitoringSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        MonitoringServiceProperties properties = new MonitoringServiceProperties();
        properties.setQualityDegradedThreshold(50);
        properties.setNotificationServiceBaseUrl("http://localhost:18086");

        sessionHealthService = new SessionHealthService(sessionHealthRepository);
        sloService = new SloService(sloWindowRepository, properties);
        alertService =
                new AlertService(alertRuleRepository, alertRepository, outboxRecorder, properties);
        snapshotService =
                new MonitoringSnapshotService(
                        sessionHealthService,
                        sloService,
                        alertService,
                        snapshotCache,
                        redisTemplate);

        when(sessionHealthRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("monitoring:waiting-queue")).thenReturn("2");
        when(sessionHealthRepository.countByStatus(SessionHealthService.STATUS_ACTIVE)).thenReturn(0L, 1L);
        when(sessionHealthRepository.findByStatus(SessionHealthService.STATUS_ACTIVE))
                .thenReturn(List.of());
        when(alertRepository.countByStatus("OPEN")).thenReturn(0L);
        when(sloWindowRepository.findFirstByOrderByWindowEndDesc()).thenReturn(Optional.of(sloWindow()));
    }

    @Test
    void snapshotChangesAfterVideoSessionStarted() {
        when(snapshotCache.get()).thenReturn(Optional.empty());

        MonitoringSnapshot before = snapshotService.snapshot();
        assertThat(before.activeSessions()).isZero();

        SessionHealthEntity active = new SessionHealthEntity();
        active.setSessionId("sess-1");
        active.setAppointmentId("appt-1");
        active.setStatus(SessionHealthService.STATUS_ACTIVE);
        active.setStartedAt(Instant.parse("2026-08-18T12:00:00Z"));
        sessionHealthService.onSessionStarted(
                "sess-1", "appt-1", "simulator", Instant.parse("2026-08-18T12:00:00Z"));

        when(sessionHealthRepository.countByStatus(SessionHealthService.STATUS_ACTIVE)).thenReturn(1L);
        MonitoringSnapshot after = snapshotService.refreshSnapshot();

        assertThat(after.activeSessions()).isEqualTo(1);
        assertThat(after.waitingRoomQueue()).isEqualTo(2);
        assertThat(after.errorRate()).isZero();
        assertThat(before).isNotEqualTo(after);
    }

    @Test
    void qualityDegradedPublishesConnectionQualityDegradedViaOutbox() {
        MonitoringServiceProperties properties = new MonitoringServiceProperties();
        properties.setQualityDegradedThreshold(50);
        properties.setNotificationServiceBaseUrl("http://localhost:18086");
        QualityService qualityService =
                new QualityService(sessionHealthService, alertService, snapshotService, properties);

        AlertRuleEntity rule = new AlertRuleEntity();
        rule.setId("rule-quality");
        rule.setMetric("quality_score");
        rule.setThreshold(50);
        rule.setComparison("LT");
        rule.setSeverity("WARNING");
        rule.setEnabled(true);
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SessionHealthEntity session = new SessionHealthEntity();
        session.setSessionId("sess-1");
        session.setAppointmentId("appt-1");
        session.setStatus(SessionHealthService.STATUS_ACTIVE);
        when(sessionHealthRepository.findById("sess-1")).thenReturn(Optional.of(session));

        qualityService.recordQuality(
                new VideoQualityRequest("sess-1", "appt-1", "PATIENT", 30));

        verify(outboxRecorder).record(anyString(), anyString(), any(), anyString());
    }

    @Test
    void doesNotPublishWhenQualityIsAcceptable() {
        MonitoringServiceProperties properties = new MonitoringServiceProperties();
        properties.setQualityDegradedThreshold(50);
        QualityService qualityService =
                new QualityService(sessionHealthService, alertService, snapshotService, properties);

        SessionHealthEntity session = new SessionHealthEntity();
        session.setSessionId("sess-1");
        session.setAppointmentId("appt-1");
        session.setStatus(SessionHealthService.STATUS_ACTIVE);
        when(sessionHealthRepository.findById("sess-1")).thenReturn(Optional.of(session));

        qualityService.recordQuality(
                new VideoQualityRequest("sess-1", "appt-1", "PATIENT", 80));

        verify(outboxRecorder, never()).record(anyString(), anyString(), any(), anyString());
    }

    private static SloWindowEntity sloWindow() {
        SloWindowEntity window = new SloWindowEntity();
        window.setId("slo-1");
        window.setWindowStart(Instant.parse("2026-08-18T11:00:00Z"));
        window.setWindowEnd(Instant.parse("2026-08-18T13:00:00Z"));
        window.setTotalRequests(10);
        window.setErrorCount(0);
        window.setBurnRate(0.0);
        window.setUpdatedAt(Instant.now());
        return window;
    }
}
