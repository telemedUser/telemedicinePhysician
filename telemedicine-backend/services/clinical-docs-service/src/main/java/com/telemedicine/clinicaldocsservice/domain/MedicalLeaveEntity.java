package com.telemedicine.clinicaldocsservice.domain;

import com.telemedicine.common.persistence.BaseEntity;
import com.telemedicine.common.persistence.encryption.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "medical_leaves")
public class MedicalLeaveEntity extends BaseEntity {

    @Column(nullable = false)
    private String appointmentId;

    @Column(nullable = false)
    private String externalCertificateId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private int durationDays;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String diagnosis;

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

    public String getExternalCertificateId() {
        return externalCertificateId;
    }

    public void setExternalCertificateId(String externalCertificateId) {
        this.externalCertificateId = externalCertificateId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
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
