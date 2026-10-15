package com.telemedicine.notificationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_log")
public class NotificationLogEntity {

    @Id
    private String id;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(nullable = false)
    private String channel;

    @Column(name = "template_key", nullable = false)
    private String templateKey;

    @Column(name = "recipient_ref", nullable = false)
    private String recipientRef;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationLogEntity() {}

    public NotificationLogEntity(
            String id,
            String appointmentId,
            String channel,
            String templateKey,
            String recipientRef,
            String status,
            int attempts,
            String lastErrorCode,
            String providerMessageId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.channel = channel;
        this.templateKey = templateKey;
        this.recipientRef = recipientRef;
        this.status = status;
        this.attempts = attempts;
        this.lastErrorCode = lastErrorCode;
        this.providerMessageId = providerMessageId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getRecipientRef() {
        return recipientRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
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
