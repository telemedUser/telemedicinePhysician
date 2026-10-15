package com.telemedicine.schedulerservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_definitions")
public class JobDefinition {

    @Id
    @Column(name = "job_key", length = 128)
    private String jobKey;

    @Column(nullable = false)
    private String description;

    @Column(name = "cron_expression", nullable = false, length = 64)
    private String cronExpression;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "target_service", length = 128)
    private String targetService;

    @Column(name = "target_path", length = 256)
    private String targetPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobDefinition() {}

    public String getJobKey() {
        return jobKey;
    }

    public String getDescription() {
        return description;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTargetService() {
        return targetService;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
