package com.telemedicine.surveyservice.domain;

import com.telemedicine.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "survey_requests")
public class SurveyRequestEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String appointmentId;

    @Column(nullable = false)
    private String surveyDefinitionId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant requestedAt;

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getSurveyDefinitionId() {
        return surveyDefinitionId;
    }

    public void setSurveyDefinitionId(String surveyDefinitionId) {
        this.surveyDefinitionId = surveyDefinitionId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }
}
