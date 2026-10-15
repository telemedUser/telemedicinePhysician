package com.telemedicine.physicianservice.domain.provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

/**
 * A recurring availability window with a per-mode concurrency limit (PRD §21, TM-PRV-002).
 * {@code dayOfWeek} is 1=Mon .. 7=Sun.
 */
@Entity
@Table(name = "availability")
public class AvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(nullable = false)
    private String market;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String mode;

    @Column(name = "max_concurrent", nullable = false)
    private int maxConcurrent = 1;

    protected AvailabilityEntity() {}

    public AvailabilityEntity(
            ProviderEntity provider,
            String market,
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String mode,
            int maxConcurrent) {
        this.provider = provider;
        this.market = market;
        this.dayOfWeek = (short) dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.mode = mode;
        this.maxConcurrent = maxConcurrent;
    }

    public short getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getMode() {
        return mode;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }
}
