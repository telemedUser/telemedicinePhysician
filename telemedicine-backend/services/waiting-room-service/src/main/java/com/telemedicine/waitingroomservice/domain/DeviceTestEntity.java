package com.telemedicine.waitingroomservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "device_tests")
public class DeviceTestEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "appointment_id", length = 128, nullable = false, unique = true)
    private String appointmentId;

    @Column(name = "patient_ref", length = 128, nullable = false)
    private String patientRef;

    @Column(name = "camera_ok", nullable = false)
    private boolean cameraOk;

    @Column(name = "microphone_ok", nullable = false)
    private boolean microphoneOk;

    @Column(name = "bandwidth_kbps", nullable = false)
    private int bandwidthKbps;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientRef() {
        return patientRef;
    }

    public void setPatientRef(String patientRef) {
        this.patientRef = patientRef;
    }

    public boolean isCameraOk() {
        return cameraOk;
    }

    public void setCameraOk(boolean cameraOk) {
        this.cameraOk = cameraOk;
    }

    public boolean isMicrophoneOk() {
        return microphoneOk;
    }

    public void setMicrophoneOk(boolean microphoneOk) {
        this.microphoneOk = microphoneOk;
    }

    public int getBandwidthKbps() {
        return bandwidthKbps;
    }

    public void setBandwidthKbps(int bandwidthKbps) {
        this.bandwidthKbps = bandwidthKbps;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
