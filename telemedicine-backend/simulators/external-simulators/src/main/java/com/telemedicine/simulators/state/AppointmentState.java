package com.telemedicine.simulators.state;

import java.time.Instant;

public record AppointmentState(
        String appointmentId,
        String status,
        String paymentStatus,
        String consentStatus,
        Instant linkedAt,
        Instant createdAt) {

    public AppointmentState withStatus(String newStatus) {
        return new AppointmentState(
                appointmentId, newStatus, paymentStatus, consentStatus, linkedAt, createdAt);
    }

    public AppointmentState withPaymentStatus(String newPaymentStatus) {
        return new AppointmentState(
                appointmentId, status, newPaymentStatus, consentStatus, linkedAt, createdAt);
    }

    public AppointmentState withLinkedAt(Instant when) {
        return new AppointmentState(
                appointmentId, status, paymentStatus, consentStatus, when, createdAt);
    }
}
