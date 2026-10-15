package com.telemedicine.auditservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consent_ledger")
public class ConsentLedgerEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "appointment_id", length = 128, nullable = false)
    private String appointmentId;

    @Column(name = "consent_type", length = 64, nullable = false)
    private String consentType;

    @Column(length = 32, nullable = false)
    private String version;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "subject_ref", length = 128)
    private String subjectRef;

    @Column(length = 128)
    private String scope;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected ConsentLedgerEntity() {}

    public ConsentLedgerEntity(
            String id,
            String appointmentId,
            String consentType,
            String version,
            boolean granted,
            Instant recordedAt,
            String subjectRef,
            String scope,
            Instant withdrawnAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.consentType = consentType;
        this.version = version;
        this.granted = granted;
        this.recordedAt = recordedAt;
        this.subjectRef = subjectRef;
        this.scope = scope;
        this.withdrawnAt = withdrawnAt;
    }

    public String getId() {
        return id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getConsentType() {
        return consentType;
    }

    public String getVersion() {
        return version;
    }

    public boolean isGranted() {
        return granted;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getSubjectRef() {
        return subjectRef;
    }

    public String getScope() {
        return scope;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }
}
