package com.telemedicine.reportingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.telemedicine.reportingservice.domain.KpiEventRepository;
import com.telemedicine.reportingservice.domain.KpiFamily;
import com.telemedicine.reportingservice.domain.KpiSnapshotRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KpiAggregationServiceTest {

    @Mock private KpiEventRepository kpiEventRepository;
    @Mock private KpiSnapshotRepository kpiSnapshotRepository;

    @InjectMocks private KpiAggregationService aggregationService;

    @Test
    void generateDailySnapshotsReturnsZeroWhenNoEvents() {
        when(kpiEventRepository.findByFamilyAndWindow(eq(KpiFamily.TECHNICAL.name()), any(), any()))
                .thenReturn(List.of());
        when(kpiEventRepository.findByFamilyAndWindow(eq(KpiFamily.FUNCTIONAL.name()), any(), any()))
                .thenReturn(List.of());
        when(kpiEventRepository.findByFamilyAndWindow(eq(KpiFamily.BUSINESS.name()), any(), any()))
                .thenReturn(List.of());
        when(kpiEventRepository.countByMetricFamilyAndMetricNameAndRecordedAtBetween(
                        anyString(), anyString(), any(), any()))
                .thenReturn(0L);

        int processed = aggregationService.generateDailySnapshots(LocalDate.now(ZoneOffset.UTC));

        assertThat(processed).isZero();
    }
}
