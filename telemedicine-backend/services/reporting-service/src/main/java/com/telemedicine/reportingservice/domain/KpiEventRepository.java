package com.telemedicine.reportingservice.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KpiEventRepository extends JpaRepository<KpiEventEntity, String> {

    @Query(
            """
            select e from KpiEventEntity e
            where e.metricFamily = :family
              and e.recordedAt >= :from
              and e.recordedAt < :to
            """)
    List<KpiEventEntity> findByFamilyAndWindow(
            @Param("family") String family, @Param("from") Instant from, @Param("to") Instant to);

    long countByMetricFamilyAndMetricNameAndRecordedAtBetween(
            String metricFamily, String metricName, Instant from, Instant to);
}
