package com.telemedicine.reportingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.reportingservice.domain.JourneyMilestoneRepository;
import com.telemedicine.reportingservice.domain.KpiEventEntity;
import com.telemedicine.reportingservice.domain.KpiEventRepository;
import com.telemedicine.reportingservice.domain.KpiFamily;
import com.telemedicine.reportingservice.domain.KpiSnapshotRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KpiRecordingServiceTest {

    @Mock private KpiEventRepository kpiEventRepository;
    @Mock private JourneyMilestoneRepository milestoneRepository;
    @Mock private KpiSnapshotRepository kpiSnapshotRepository;

    private KpiRecordingService recordingService;
    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        recordingService = new KpiRecordingService(kpiEventRepository, milestoneRepository);
        reportingService =
                new ReportingService(new KpiAggregationService(kpiEventRepository, kpiSnapshotRepository));
        when(kpiEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void computesPatientNpsFromSurveyEventsNotHardcodedValues() {
        Instant now = Instant.parse("2026-08-18T10:00:00Z");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = Instant.now();

        KpiEventEntity first = event(KpiFamily.BUSINESS, KpiRecordingService.METRIC_PATIENT_NPS, 9.0, now);
        KpiEventEntity second = event(KpiFamily.BUSINESS, KpiRecordingService.METRIC_PATIENT_NPS, 7.0, now);
        when(kpiEventRepository.findByFamilyAndWindow(KpiFamily.BUSINESS.name(), from, to))
                .thenReturn(List.of(first, second));
        when(kpiSnapshotRepository.findBySnapshotDateAndMetricFamily(today, KpiFamily.BUSINESS.name()))
                .thenReturn(List.of());

        var response = reportingService.familyKpis("business");

        assertThat(response.metrics()).containsEntry(KpiRecordingService.METRIC_PATIENT_NPS, 8.0);
        assertThat(response.metrics()).doesNotContainKey("patientNps");
    }

    @Test
    void recordsConsultationCompletedUsingJourneyMetricsName() {
        ArgumentCaptor<KpiEventEntity> captor = ArgumentCaptor.forClass(KpiEventEntity.class);
        recordingService.recordConsultationClosed(
                "evt-closed", "appt-1", "COMPLETED", Instant.parse("2026-08-18T11:00:00Z"));

        verify(kpiEventRepository).save(captor.capture());
        assertThat(captor.getValue().getMetricName())
                .isEqualTo(JourneyMetrics.METRIC_CONSULTATION_COMPLETED);
    }

    @Test
    void exposesNullPhysicianSatisfactionWithoutFakeScore() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(kpiEventRepository.findByFamilyAndWindow(
                        eq(KpiFamily.BUSINESS.name()), any(), any()))
                .thenReturn(List.of());
        when(kpiSnapshotRepository.findBySnapshotDateAndMetricFamily(today, KpiFamily.BUSINESS.name()))
                .thenReturn(List.of());

        var response = reportingService.familyKpis("business");

        assertThat(response.metrics()).containsEntry("telemedicine.journey.physician.satisfaction", null);
    }

    private static KpiEventEntity event(KpiFamily family, String metric, double value, Instant recordedAt) {
        KpiEventEntity entity = new KpiEventEntity();
        entity.setMetricFamily(family.name());
        entity.setMetricName(metric);
        entity.setNumericValue(value);
        entity.setRecordedAt(recordedAt);
        return entity;
    }
}
