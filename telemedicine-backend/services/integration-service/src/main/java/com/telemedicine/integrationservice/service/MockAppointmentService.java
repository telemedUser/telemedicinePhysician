package com.telemedicine.integrationservice.service;

import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.integrationservice.domain.*;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockAppointmentService implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final MpiPatientRepository mpiPatientRepository;

    public MockAppointmentService(
            AppointmentRepository appointmentRepository, MpiPatientRepository mpiPatientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.mpiPatientRepository = mpiPatientRepository;
    }

    @Override
    public Appointment create(Map<String, Object> body) {
        String id = "apt-" + IdGenerator.newId();
        Appointment appointment =
                new Appointment(
                        id,
                        stringOrDefault(body, "patientId", "pat-unknown"),
                        stringOrDefault(body, "physicianId", "phy-001"),
                        AppointmentStatus.CREATED,
                        java.time.Instant.now().toString(),
                        "PENDING",
                        "sim-his");
        return appointmentRepository.save(appointment);
    }

    @Override
    public Appointment consult(String id) {
        return appointmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Appointment not found: " + id));
    }

    @Override
    public Appointment updateStatus(String id, String status) {
        Appointment existing = consult(id);
        AppointmentStatus parsed = AppointmentStatus.valueOf(status.toUpperCase());
        return appointmentRepository.save(existing.withStatus(parsed));
    }

    @Override
    public Appointment reschedule(String id, String newSlot) {
        Appointment existing = consult(id);
        return appointmentRepository.save(
                existing.withScheduledAt(newSlot).withStatus(AppointmentStatus.RESCHEDULED));
    }

    @Override
    public Appointment validatePayment(String id) {
        Appointment existing = consult(id);
        if (!"CONFIRMED".equalsIgnoreCase(existing.paymentStatus())) {
            return appointmentRepository.save(existing.withPaymentStatus("CONFIRMED"));
        }
        return existing;
    }

    @Override
    public MpiPatient mpiLookup(String appointmentId) {
        consult(appointmentId);
        return mpiPatientRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(
                        () -> new DomainException(ErrorCode.NOT_FOUND, "MPI record not found: " + appointmentId));
    }

    private static String stringOrDefault(Map<String, Object> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
