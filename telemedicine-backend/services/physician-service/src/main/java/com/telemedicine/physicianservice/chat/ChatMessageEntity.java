package com.telemedicine.physicianservice.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A clinical chat message within a consultation (PRD §13.3). The transcript is retained as clinical
 * data, longitudinal from waiting-room entry to closure, and is exportable into documentation. The
 * body lives here in Postgres; only a PHI-free {@code ChatMessageSent} marker is published.
 */
@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {

    @Id
    private String id;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Column(name = "sender_role", nullable = false)
    private String senderRole;

    @Column(name = "sender_ref")
    private String senderRef;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean clinical;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected ChatMessageEntity() {}

    public ChatMessageEntity(
            String id,
            String appointmentId,
            String senderRole,
            String senderRef,
            String body,
            boolean clinical,
            Instant sentAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.senderRole = senderRole;
        this.senderRef = senderRef;
        this.body = body;
        this.clinical = clinical;
        this.sentAt = sentAt;
    }

    public String getId() {
        return id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public String getSenderRef() {
        return senderRef;
    }

    public String getBody() {
        return body;
    }

    public boolean isClinical() {
        return clinical;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
