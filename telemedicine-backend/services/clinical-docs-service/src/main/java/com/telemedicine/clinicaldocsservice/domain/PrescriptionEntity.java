package com.telemedicine.clinicaldocsservice.domain;

import com.telemedicine.common.persistence.BaseEntity;
import com.telemedicine.common.persistence.encryption.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "prescriptions")
public class PrescriptionEntity extends BaseEntity {

    @Column(nullable = false)
    private String appointmentId;

    @Column(nullable = false)
    private String externalPrescriptionId;

    @Column(nullable = false)
    private String provider;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String medications;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String physicianRef;

    @Column(nullable = false)
    private Instant issuedAt;

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getExternalPrescriptionId() {
        return externalPrescriptionId;
    }

    public void setExternalPrescriptionId(String externalPrescriptionId) {
        this.externalPrescriptionId = externalPrescriptionId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPhysicianRef() {
        return physicianRef;
    }

    public void setPhysicianRef(String physicianRef) {
        this.physicianRef = physicianRef;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }
}
