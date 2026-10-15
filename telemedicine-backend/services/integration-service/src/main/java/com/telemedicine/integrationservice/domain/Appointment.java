package com.telemedicine.integrationservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Appointment(
        String id,
        String patientId,
        String physicianId,
        AppointmentStatus status,
        String scheduledAt,
        String paymentStatus,
        String source,
        @JsonIgnore Instant consultationClosedAt) {

    public Appointment(
            String id,
            String patientId,
            String physicianId,
            AppointmentStatus status,
            String scheduledAt,
            String paymentStatus,
            String source) {
        this(id, patientId, physicianId, status, scheduledAt, paymentStatus, source, null);
    }

    public Appointment withStatus(AppointmentStatus newStatus) {
        return new Appointment(
                id, patientId, physicianId, newStatus, scheduledAt, paymentStatus, source, consultationClosedAt);
    }

    public Appointment withPaymentStatus(String newPaymentStatus) {
        return new Appointment(
                id, patientId, physicianId, status, scheduledAt, newPaymentStatus, source, consultationClosedAt);
    }

    public Appointment withScheduledAt(String newScheduledAt) {
        return new Appointment(
                id, patientId, physicianId, status, newScheduledAt, paymentStatus, source, consultationClosedAt);
    }

    public Appointment withConsultationClosedAt(Instant closedAt) {
        return new Appointment(
                id, patientId, physicianId, status, scheduledAt, paymentStatus, source, closedAt);
    }
}
