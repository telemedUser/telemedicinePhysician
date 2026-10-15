package com.telemedicine.identityservice.domain;

import com.telemedicine.common.persistence.encryption.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fonasa_challenges")
public class FonasaChallengeEntity {

    @Id
    private String id;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Column(name = "patient_ref", nullable = false)
    private String patientRef;

    @Column(name = "masked_phone")
    private String maskedPhone;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "rut_encrypted")
    private String rutEncrypted;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_encrypted")
    private String phoneEncrypted;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "lockout_until")
    private Instant lockoutUntil;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FonasaChallengeEntity() {}

    public FonasaChallengeEntity(
            String id,
            String appointmentId,
            String patientRef,
            String maskedPhone,
            String rutEncrypted,
            String phoneEncrypted,
            int attempts,
            Instant lockoutUntil,
            boolean verified,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientRef = patientRef;
        this.maskedPhone = maskedPhone;
        this.rutEncrypted = rutEncrypted;
        this.phoneEncrypted = phoneEncrypted;
        this.attempts = attempts;
        this.lockoutUntil = lockoutUntil;
        this.verified = verified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public String getRutEncrypted() {
        return rutEncrypted;
    }

    public String getPhoneEncrypted() {
        return phoneEncrypted;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getLockoutUntil() {
        return lockoutUntil;
    }

    public void setLockoutUntil(Instant lockoutUntil) {
        this.lockoutUntil = lockoutUntil;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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
