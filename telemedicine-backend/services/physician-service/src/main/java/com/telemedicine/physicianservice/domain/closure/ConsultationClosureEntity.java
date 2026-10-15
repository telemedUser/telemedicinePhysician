package com.telemedicine.physicianservice.domain.closure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The closure fact for a consultation (PRD §14.2). This service owns the record of the closure;
 * the documents themselves remain in their systems of record (Aidbox, ReceMed, Medipass).
 * {@code ConsultationClosed} is emitted only when the minimum completion set is satisfied or an
 * audited exception is recorded (TM-DOC-009).
 */
@Entity
@Table(name = "consultation_closure")
public class ConsultationClosureEntity {

    @Id
    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    private String market;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** JSON object of booleans keyed by document type (note, diagnosis, prescription, ...). */
    @Column(name = "completion_set", columnDefinition = "text")
    private String completionSet;

    @Column(name = "minimum_satisfied", nullable = false)
    private boolean minimumSatisfied;

    @Column(name = "exception_reason")
    private String exceptionReason;

    @Column(name = "document_refs", columnDefinition = "text")
    private String documentRefs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConsultationClosureEntity() {}

    public ConsultationClosureEntity(String appointmentId, String providerId, String market) {
        this.appointmentId = appointmentId;
        this.providerId = providerId;
        this.market = market;
        this.minimumSatisfied = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getMarket() {
        return market;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getCompletionSet() {
        return completionSet;
    }

    public void setCompletionSet(String completionSet) {
        this.completionSet = completionSet;
    }

    public boolean isMinimumSatisfied() {
        return minimumSatisfied;
    }

    public void setMinimumSatisfied(boolean minimumSatisfied) {
        this.minimumSatisfied = minimumSatisfied;
    }

    public String getExceptionReason() {
        return exceptionReason;
    }

    public void setExceptionReason(String exceptionReason) {
        this.exceptionReason = exceptionReason;
    }

    public String getDocumentRefs() {
        return documentRefs;
    }

    public void setDocumentRefs(String documentRefs) {
        this.documentRefs = documentRefs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
