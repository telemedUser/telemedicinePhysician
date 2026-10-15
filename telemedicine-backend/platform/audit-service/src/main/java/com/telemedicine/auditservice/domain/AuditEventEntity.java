package com.telemedicine.auditservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "actor_id", length = 128, nullable = false)
    private String actorId;

    @Column(name = "actor_role", length = 64, nullable = false)
    private String actorRole;

    @Column(length = 128, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 128, nullable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 128, nullable = false)
    private String resourceId;

    @Column(length = 32, nullable = false)
    private String outcome;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_ip", length = 45)
    private String actorIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 512)
    private String reason;

    @Column(nullable = false)
    private long sequence;

    @Column(name = "previous_hash", length = 128, nullable = false)
    private String previousHash;

    @Column(length = 128, nullable = false)
    private String hash;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    protected AuditEventEntity() {}

    public AuditEventEntity(
            String id,
            String actorId,
            String actorRole,
            String action,
            String resourceType,
            String resourceId,
            String outcome,
            Instant occurredAt,
            String actorIp,
            String userAgent,
            String reason,
            long sequence,
            String previousHash,
            String hash,
            String correlationId) {
        this.id = id;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.occurredAt = occurredAt;
        this.actorIp = actorIp;
        this.userAgent = userAgent;
        this.reason = reason;
        this.sequence = sequence;
        this.previousHash = previousHash;
        this.hash = hash;
        this.correlationId = correlationId;
    }

    public String getId() {
        return id;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getOutcome() {
        return outcome;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActorIp() {
        return actorIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReason() {
        return reason;
    }

    public long getSequence() {
        return sequence;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
