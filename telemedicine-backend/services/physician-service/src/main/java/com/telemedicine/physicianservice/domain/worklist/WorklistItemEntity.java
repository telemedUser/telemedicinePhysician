package com.telemedicine.physicianservice.domain.worklist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Derived read model of a patient on a physician's worklist (PRD §12.3). Rebuilt from the event
 * stream (appointment, waiting-room and quality events); the only API-driven writes are the
 * red-flag acknowledgement (TM-WLT-009) and operator force-transition (TM-WRM-007).
 */
@Entity
@Table(name = "worklist_item")
public class WorklistItemEntity {

    @Id
    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    private String market;

    @Column(name = "patient_ref")
    private String patientRef;

    private String mode;

    @Column(name = "entered_at")
    private Instant enteredAt;

    /** The slot the patient booked (AppointmentCreated.scheduledAt), shown as the clinic "Time". */
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(nullable = false)
    private String readiness;

    @Column(name = "consultation_status", nullable = false)
    private String consultationStatus;

    @Column(name = "technical_status", nullable = false)
    private String technicalStatus;

    @Column(name = "telemetry_available", nullable = false)
    private boolean telemetryAvailable;

    /** JSON array of flags: FIRST_VISIT, FOLLOW_UP, GUEST, RED_FLAG. */
    @Column(columnDefinition = "text")
    private String flags;

    @Column(name = "red_flag_acknowledged_at")
    private Instant redFlagAcknowledgedAt;

    @Column(name = "red_flag_acknowledged_by")
    private String redFlagAcknowledgedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorklistItemEntity() {}

    public WorklistItemEntity(String appointmentId, String market) {
        this.appointmentId = appointmentId;
        this.market = market;
        this.readiness = "NOT_READY";
        this.consultationStatus = "CHECKED_IN";
        this.technicalStatus = "OK";
        this.telemetryAvailable = false;
        this.updatedAt = Instant.now();
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getMarket() {
        return market;
    }

    public String getPatientRef() {
        return patientRef;
    }

    public void setPatientRef(String patientRef) {
        this.patientRef = patientRef;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Instant getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(Instant enteredAt) {
        this.enteredAt = enteredAt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getReadiness() {
        return readiness;
    }

    public void setReadiness(String readiness) {
        this.readiness = readiness;
    }

    public String getConsultationStatus() {
        return consultationStatus;
    }

    public void setConsultationStatus(String consultationStatus) {
        this.consultationStatus = consultationStatus;
    }

    public String getTechnicalStatus() {
        return technicalStatus;
    }

    public void setTechnicalStatus(String technicalStatus) {
        this.technicalStatus = technicalStatus;
    }

    public boolean isTelemetryAvailable() {
        return telemetryAvailable;
    }

    public void setTelemetryAvailable(boolean telemetryAvailable) {
        this.telemetryAvailable = telemetryAvailable;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public Instant getRedFlagAcknowledgedAt() {
        return redFlagAcknowledgedAt;
    }

    public void setRedFlagAcknowledgedAt(Instant redFlagAcknowledgedAt) {
        this.redFlagAcknowledgedAt = redFlagAcknowledgedAt;
    }

    public String getRedFlagAcknowledgedBy() {
        return redFlagAcknowledgedBy;
    }

    public void setRedFlagAcknowledgedBy(String redFlagAcknowledgedBy) {
        this.redFlagAcknowledgedBy = redFlagAcknowledgedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
