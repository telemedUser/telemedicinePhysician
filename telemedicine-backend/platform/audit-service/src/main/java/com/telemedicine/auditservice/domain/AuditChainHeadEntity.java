package com.telemedicine.auditservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_chain_head")
public class AuditChainHeadEntity {

    public static final String GLOBAL_ID = "GLOBAL";

    @Id
    @Column(length = 32, nullable = false)
    private String id;

    @Column(name = "last_sequence", nullable = false)
    private long lastSequence;

    @Column(name = "last_hash", length = 128, nullable = false)
    private String lastHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuditChainHeadEntity() {}

    public AuditChainHeadEntity(String id, long lastSequence, String lastHash, Instant updatedAt) {
        this.id = id;
        this.lastSequence = lastSequence;
        this.lastHash = lastHash;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public long getLastSequence() {
        return lastSequence;
    }

    public String getLastHash() {
        return lastHash;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void advance(long sequence, String hash, Instant updatedAt) {
        this.lastSequence = sequence;
        this.lastHash = hash;
        this.updatedAt = updatedAt;
    }
}
