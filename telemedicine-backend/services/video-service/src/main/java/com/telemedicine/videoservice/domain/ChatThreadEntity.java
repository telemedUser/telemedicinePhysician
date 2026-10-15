package com.telemedicine.videoservice.domain;

import com.telemedicine.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_threads")
public class ChatThreadEntity extends BaseEntity {

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String appointmentId;

    @Column(nullable = false)
    private String provider;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

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
}
