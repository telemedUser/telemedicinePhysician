package com.telemedicine.integrationservice.infrastructure.persistence;

import com.telemedicine.integrationservice.domain.Appointment;
import com.telemedicine.integrationservice.domain.AppointmentStatus;
import java.time.Instant;

final class AppointmentMapper {

    private AppointmentMapper() {}

    static Appointment toDomain(AppointmentEntity entity) {
        return new Appointment(
                entity.getId(),
                entity.getPatientId(),
                entity.getPhysicianId(),
                entity.getStatus(),
                entity.getScheduledAt() != null ? entity.getScheduledAt().toString() : null,
                entity.getPaymentStatus(),
                entity.getSource(),
                entity.getConsultationClosedAt());
    }

    static AppointmentEntity toNewEntity(Appointment appointment, Instant now) {
        AppointmentEntity entity = new AppointmentEntity();
        entity.setId(appointment.id());
        entity.setCreatedAt(now);
        entity.setConsultationClosedAt(appointment.consultationClosedAt());
        mergeFields(entity, appointment, now);
        return entity;
    }

    static AppointmentEntity merge(
            AppointmentEntity entity, Appointment appointment, Instant now) {
        mergeFields(entity, appointment, now);
        if (appointment.consultationClosedAt() != null) {
            entity.setConsultationClosedAt(appointment.consultationClosedAt());
        }
        return entity;
    }

    private static void mergeFields(AppointmentEntity entity, Appointment appointment, Instant now) {
        entity.setPatientId(appointment.patientId());
        entity.setPhysicianId(appointment.physicianId());
        entity.setStatus(appointment.status());
        entity.setScheduledAt(parseInstant(appointment.scheduledAt()));
        entity.setPaymentStatus(appointment.paymentStatus());
        entity.setSource(appointment.source());
        entity.setUpdatedAt(now);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
