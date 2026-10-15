package com.telemedicine.reportingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "kpi_snapshots")
public class KpiSnapshotEntity {

    @Id
    private String id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "metric_family", nullable = false)
    private String metricFamily;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "aggregate_value")
    private Double aggregateValue;

    @Column(name = "sample_count", nullable = false)
    private long sampleCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getMetricFamily() {
        return metricFamily;
    }

    public void setMetricFamily(String metricFamily) {
        this.metricFamily = metricFamily;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Double getAggregateValue() {
        return aggregateValue;
    }

    public void setAggregateValue(Double aggregateValue) {
        this.aggregateValue = aggregateValue;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(long sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
