package com.telemedicine.reportingservice.service;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.reportingservice.domain.KpiEventEntity;
import com.telemedicine.reportingservice.domain.KpiEventRepository;
import com.telemedicine.reportingservice.domain.KpiFamily;
import com.telemedicine.reportingservice.domain.KpiSnapshotEntity;
import com.telemedicine.reportingservice.domain.KpiSnapshotRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KpiAggregationService {

    private final KpiEventRepository kpiEventRepository;
    private final KpiSnapshotRepository kpiSnapshotRepository;

    public KpiAggregationService(
            KpiEventRepository kpiEventRepository, KpiSnapshotRepository kpiSnapshotRepository) {
        this.kpiEventRepository = kpiEventRepository;
        this.kpiSnapshotRepository = kpiSnapshotRepository;
    }

    @Transactional
    public int generateDailySnapshots(LocalDate snapshotDate) {
        Instant from = snapshotDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = snapshotDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        int written = 0;
        for (KpiFamily family : KpiFamily.values()) {
            List<KpiEventEntity> events =
                    kpiEventRepository.findByFamilyAndWindow(family.name(), from, to);
            Map<String, AggregateAccumulator> aggregates = new HashMap<>();
            for (KpiEventEntity event : events) {
                aggregates
                        .computeIfAbsent(event.getMetricName(), ignored -> new AggregateAccumulator())
                        .add(event.getNumericValue());
            }
            for (Map.Entry<String, AggregateAccumulator> entry : aggregates.entrySet()) {
                upsertSnapshot(snapshotDate, family, entry.getKey(), entry.getValue());
                written++;
            }
        }
        upsertDerivedTechnicalSnapshots(snapshotDate, from, to);
        return written;
    }

    public Map<String, Double> currentMetrics(KpiFamily family) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<KpiSnapshotEntity> snapshots =
                kpiSnapshotRepository.findBySnapshotDateAndMetricFamily(today, family.name());
        if (snapshots.isEmpty()) {
            return computeLiveMetrics(family, today);
        }
        Map<String, Double> metrics = new HashMap<>();
        for (KpiSnapshotEntity snapshot : snapshots) {
            metrics.put(snapshot.getMetricName(), snapshot.getAggregateValue());
        }
        applyDerivedMetrics(family, metrics, today);
        return metrics;
    }

    public Map<String, Double> allCurrentMetrics() {
        Map<String, Double> all = new HashMap<>();
        for (KpiFamily family : KpiFamily.values()) {
            currentMetrics(family).forEach((key, value) -> all.put(key, value));
        }
        return all;
    }

    private Map<String, Double> computeLiveMetrics(KpiFamily family, LocalDate day) {
        Instant from = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = Instant.now();
        List<KpiEventEntity> events = kpiEventRepository.findByFamilyAndWindow(family.name(), from, to);
        Map<String, AggregateAccumulator> aggregates = new HashMap<>();
        for (KpiEventEntity event : events) {
            aggregates.computeIfAbsent(event.getMetricName(), ignored -> new AggregateAccumulator())
                    .add(event.getNumericValue());
        }
        Map<String, Double> metrics = new HashMap<>();
        aggregates.forEach(
                (metric, accumulator) -> metrics.put(metric, accumulator.average()));
        applyDerivedMetrics(family, metrics, day);
        return metrics;
    }

    private void applyDerivedMetrics(KpiFamily family, Map<String, Double> metrics, LocalDate day) {
        if (family != KpiFamily.FUNCTIONAL && family != KpiFamily.BUSINESS) {
            return;
        }
        Instant from = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (family == KpiFamily.FUNCTIONAL) {
            double successes =
                    kpiEventRepository.countByMetricFamilyAndMetricNameAndRecordedAtBetween(
                            KpiFamily.FUNCTIONAL.name(),
                            KpiRecordingService.METRIC_PAYMENT_SUCCESS,
                            from,
                            to);
            double attempts =
                    kpiEventRepository.countByMetricFamilyAndMetricNameAndRecordedAtBetween(
                            KpiFamily.FUNCTIONAL.name(),
                            KpiRecordingService.METRIC_PAYMENT_ATTEMPT,
                            from,
                            to);
            if (attempts > 0) {
                metrics.put("telemedicine.journey.payment.success.rate", successes / attempts);
            }
            double completed =
                    kpiEventRepository.countByMetricFamilyAndMetricNameAndRecordedAtBetween(
                            KpiFamily.FUNCTIONAL.name(),
                            JourneyMetrics.METRIC_CONSULTATION_COMPLETED,
                            from,
                            to);
            double abandoned =
                    kpiEventRepository.countByMetricFamilyAndMetricNameAndRecordedAtBetween(
                            KpiFamily.FUNCTIONAL.name(),
                            JourneyMetrics.METRIC_CONSULTATION_ABANDONED,
                            from,
                            to);
            double total = completed + abandoned;
            if (total > 0) {
                metrics.put("telemedicine.journey.consultation.completion.rate", completed / total);
            }
        }
        if (family == KpiFamily.BUSINESS) {
            // Physician satisfaction remains null until a source event exists.
        }
    }

    private void upsertDerivedTechnicalSnapshots(LocalDate snapshotDate, Instant from, Instant to) {
        long incidents =
                kpiEventRepository.countByMetricFamilyAndMetricNameAndRecordedAtBetween(
                        KpiFamily.TECHNICAL.name(),
                        KpiRecordingService.METRIC_INTEGRATION_INCIDENT,
                        from,
                        to);
        double availability = incidents == 0 ? 100.0 : Math.max(0.0, 100.0 - incidents);
        upsertSnapshot(
                snapshotDate,
                KpiFamily.TECHNICAL,
                KpiRecordingService.METRIC_AVAILABILITY,
                new AggregateAccumulator().add(availability));
        upsertSnapshot(
                snapshotDate,
                KpiFamily.TECHNICAL,
                KpiRecordingService.METRIC_INTEGRATION_INCIDENT,
                new AggregateAccumulator().add((double) incidents));
    }

    private void upsertSnapshot(
            LocalDate snapshotDate, KpiFamily family, String metricName, AggregateAccumulator accumulator) {
        KpiSnapshotEntity entity =
                kpiSnapshotRepository
                        .findBySnapshotDateAndMetricFamilyAndMetricName(
                                snapshotDate, family.name(), metricName)
                        .orElseGet(
                                () -> {
                                    KpiSnapshotEntity created = new KpiSnapshotEntity();
                                    created.setId(IdGenerator.newUlid());
                                    created.setSnapshotDate(snapshotDate);
                                    created.setMetricFamily(family.name());
                                    created.setMetricName(metricName);
                                    return created;
                                });
        entity.setAggregateValue(accumulator.average());
        entity.setSampleCount(accumulator.count());
        entity.setUpdatedAt(Instant.now());
        kpiSnapshotRepository.save(entity);
    }

    private static final class AggregateAccumulator {
        private double sum;
        private long count;

        AggregateAccumulator add(Double value) {
            if (value != null) {
                sum += value;
                count++;
            }
            return this;
        }

        double average() {
            return count == 0 ? 0.0 : sum / count;
        }

        long count() {
            return count;
        }
    }
}
