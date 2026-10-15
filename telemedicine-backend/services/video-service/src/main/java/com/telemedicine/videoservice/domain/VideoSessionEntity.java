package com.telemedicine.videoservice.domain;

import com.telemedicine.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "video_sessions")
public class VideoSessionEntity extends BaseEntity {

    @Column(nullable = false)
    private String appointmentId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerSessionId;

    private String recordingPolicy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoSessionStatus status = VideoSessionStatus.CREATED;

    private Instant startedAt;
    private Instant endedAt;
    private String endReason;
    private Instant expiresAt;

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public void setProviderSessionId(String providerSessionId) {
        this.providerSessionId = providerSessionId;
    }

    public String getRecordingPolicy() {
        return recordingPolicy;
    }

    public void setRecordingPolicy(String recordingPolicy) {
        this.recordingPolicy = recordingPolicy;
    }

    public VideoSessionStatus getStatus() {
        return status;
    }

    public void setStatus(VideoSessionStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
