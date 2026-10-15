package com.telemedicine.surveyservice.domain;

import com.telemedicine.common.persistence.BaseEntity;
import com.telemedicine.common.persistence.encryption.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "survey_responses")
public class SurveyResponseEntity extends BaseEntity {

    @Column(nullable = false)
    private String surveyRequestId;

    @Column(nullable = false, unique = true)
    private String appointmentId;

    @Column(nullable = false)
    private int score;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Instant completedAt;

    public String getSurveyRequestId() {
        return surveyRequestId;
    }

    public void setSurveyRequestId(String surveyRequestId) {
        this.surveyRequestId = surveyRequestId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
