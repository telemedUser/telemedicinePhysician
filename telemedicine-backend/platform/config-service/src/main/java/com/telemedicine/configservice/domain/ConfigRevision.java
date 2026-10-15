package com.telemedicine.configservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "config_revisions")
public class ConfigRevision {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "config_key", nullable = false, length = 128)
    private String configKey;

    @Column(name = "previous_value_hash", length = 64)
    private String previousValueHash;

    @Column(name = "new_value_hash", nullable = false, length = 64)
    private String newValueHash;

    @Column(name = "changed_by", nullable = false, length = 128)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getPreviousValueHash() {
        return previousValueHash;
    }

    public void setPreviousValueHash(String previousValueHash) {
        this.previousValueHash = previousValueHash;
    }

    public String getNewValueHash() {
        return newValueHash;
    }

    public void setNewValueHash(String newValueHash) {
        this.newValueHash = newValueHash;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
