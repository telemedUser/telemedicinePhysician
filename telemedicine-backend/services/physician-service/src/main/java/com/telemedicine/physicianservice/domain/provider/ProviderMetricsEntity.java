package com.telemedicine.physicianservice.domain.provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Per-physician productivity metrics (PRD §21, TM-PRV-007). In the full design these are derived
 * from the canonical event stream (TM-ARC-021); this table holds the current rollup, keyed by
 * provider id.
 */
@Entity
@Table(name = "provider_metrics")
public class ProviderMetricsEntity {

    @Id
    @Column(name = "provider_id")
    private String providerId;

    private double utilization;

    @Column(name = "avg_duration_seconds")
    private long avgDurationSeconds;

    @Column(name = "avg_documentation_seconds")
    private long avgDocumentationSeconds;

    @Column(name = "no_show_rate")
    private double noShowRate;

    @Column(name = "technical_failure_rate")
    private double technicalFailureRate;

    protected ProviderMetricsEntity() {}

    public String getProviderId() {
        return providerId;
    }

    public double getUtilization() {
        return utilization;
    }

    public long getAvgDurationSeconds() {
        return avgDurationSeconds;
    }

    public long getAvgDocumentationSeconds() {
        return avgDocumentationSeconds;
    }

    public double getNoShowRate() {
        return noShowRate;
    }

    public double getTechnicalFailureRate() {
        return technicalFailureRate;
    }
}
