package com.telemedicine.schedulerservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_runs")
public class JobRun {

    public enum Status {
        RUNNING,
        SUCCESS,
        FAILED,
        SKIPPED
    }

    public enum TriggerType {
        SCHEDULED,
        MANUAL
    }

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "job_key", nullable = false, length = 128)
    private String jobKey;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private TriggerType triggerType;

    protected JobRun() {}

    public JobRun(
            String id,
            String jobKey,
            Instant startedAt,
            Status status,
            TriggerType triggerType) {
        this.id = id;
        this.jobKey = jobKey;
        this.startedAt = startedAt;
        this.status = status;
        this.triggerType = triggerType;
    }

    public String getId() {
        return id;
    }

    public String getJobKey() {
        return jobKey;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void complete(Status status, String message) {
        this.status = status;
        this.message = message;
        this.finishedAt = Instant.now();
    }
}
