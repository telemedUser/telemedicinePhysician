package com.telemedicine.reportingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "journey_milestones")
@IdClass(JourneyMilestoneEntity.JourneyMilestoneId.class)
public class JourneyMilestoneEntity {

    @Id
    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Id
    @Column(name = "milestone", nullable = false)
    private String milestone;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getMilestone() {
        return milestone;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public static class JourneyMilestoneId implements Serializable {
        private String appointmentId;
        private String milestone;

        public JourneyMilestoneId() {}

        public JourneyMilestoneId(String appointmentId, String milestone) {
            this.appointmentId = appointmentId;
            this.milestone = milestone;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof JourneyMilestoneId that)) {
                return false;
            }
            return Objects.equals(appointmentId, that.appointmentId)
                    && Objects.equals(milestone, that.milestone);
        }

        @Override
        public int hashCode() {
            return Objects.hash(appointmentId, milestone);
        }
    }
}
