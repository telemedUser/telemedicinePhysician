package com.telemedicine.reportingservice.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiSnapshotRepository extends JpaRepository<KpiSnapshotEntity, String> {

    List<KpiSnapshotEntity> findBySnapshotDate(LocalDate snapshotDate);

    List<KpiSnapshotEntity> findBySnapshotDateAndMetricFamily(
            LocalDate snapshotDate, String metricFamily);

    Optional<KpiSnapshotEntity> findBySnapshotDateAndMetricFamilyAndMetricName(
            LocalDate snapshotDate, String metricFamily, String metricName);
}
