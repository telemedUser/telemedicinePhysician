package com.telemedicine.identityservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "identity_verifications")
public class IdentityVerificationEntity {

    @Id
    private String id;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Column(name = "patient_ref", nullable = false)
    private String patientRef;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private boolean validated;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentityVerificationEntity() {}

    public IdentityVerificationEntity(
            String id,
            String appointmentId,
            String patientRef,
            String method,
            boolean validated,
            Instant validatedAt,
            Instant createdAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientRef = patientRef;
        this.method = method;
        this.validated = validated;
        this.validatedAt = validatedAt;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientRef() {
        return patientRef;
    }

    public String getMethod() {
        return method;
    }

    public boolean isValidated() {
        return validated;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
