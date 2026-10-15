package com.telemedicine.integrationservice.service;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.AppointmentCreated;
import com.telemedicine.common.events.payload.AppointmentRescheduled;
import com.telemedicine.common.events.payload.ConsultationClosed;
import com.telemedicine.common.events.payload.PaymentConfirmed;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.integrationservice.adapter.HisAdapter;
import com.telemedicine.integrationservice.adapter.MpiAdapter;
import com.telemedicine.integrationservice.adapter.PaymentAdapter;
import com.telemedicine.integrationservice.domain.Appointment;
import com.telemedicine.integrationservice.domain.AppointmentRepository;
import com.telemedicine.integrationservice.domain.AppointmentStatus;
import com.telemedicine.integrationservice.domain.MpiPatient;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("postgres")
public class PostgresAppointmentService implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final OutboxRecorder outboxRecorder;
    private final HisAdapter hisAdapter;
    private final PaymentAdapter paymentAdapter;
    private final MpiAdapter mpiAdapter;

    public PostgresAppointmentService(
            AppointmentRepository appointmentRepository,
            OutboxRecorder outboxRecorder,
            HisAdapter hisAdapter,
            PaymentAdapter paymentAdapter,
            MpiAdapter mpiAdapter) {
        this.appointmentRepository = appointmentRepository;
        this.outboxRecorder = outboxRecorder;
        this.hisAdapter = hisAdapter;
        this.paymentAdapter = paymentAdapter;
        this.mpiAdapter = mpiAdapter;
    }

    @Override
    @Transactional
    public Appointment create(Map<String, Object> body) {
        Instant scheduledAt = Instant.now();
        String id = "apt-" + IdGenerator.newId();
        Appointment appointment =
                new Appointment(
                        id,
                        stringOrDefault(body, "patientId", "pat-unknown"),
                        stringOrDefault(body, "physicianId", "phy-001"),
                        AppointmentStatus.CREATED,
                        scheduledAt.toString(),
                        "PENDING",
                        stringOrDefault(body, "source", "his"));
        Appointment saved = appointmentRepository.save(appointment);
        outboxRecorder.record(
                "Appointment",
                saved.id(),
                new AppointmentCreated(
                        saved.id(),
                        saved.patientId(),
                        saved.physicianId(),
                        scheduledAt,
                        stringOrDefault(body, "modality", "VIDEO"),
                        saved.source()),
                correlationId(),
                saved.id());
        return saved;
    }

    @Override
    public Appointment consult(String id) {
        return appointmentRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new DomainException(
                                        ErrorCode.APPOINTMENT_NOT_FOUND, "Appointment not found: " + id));
    }

    @Override
    @Transactional
    public Appointment updateStatus(String id, String status) {
        Appointment existing = consult(id);
        AppointmentStatus parsed = AppointmentStatus.valueOf(status.toUpperCase());
        hisAdapter.updateStatus(id, parsed.name());
        Appointment updated = appointmentRepository.save(existing.withStatus(parsed));
        if (parsed == AppointmentStatus.COMPLETED && existing.consultationClosedAt() == null) {
            Instant closedAt = Instant.now();
            updated =
                    appointmentRepository.save(
                            updated.withStatus(AppointmentStatus.COMPLETED).withConsultationClosedAt(closedAt));
            outboxRecorder.record(
                    "Appointment",
                    id,
                    new ConsultationClosed(id, "COMPLETED", 0L, closedAt),
                    correlationId(),
                    id);
        }
        return updated;
    }

    @Override
    @Transactional
    public Appointment reschedule(String id, String newSlot) {
        Appointment existing = consult(id);
        Instant previousScheduledAt = parseInstant(existing.scheduledAt());
        Instant scheduledAt = parseInstant(newSlot);
        Appointment updated =
                appointmentRepository.save(
                        existing.withScheduledAt(scheduledAt.toString())
                                .withStatus(AppointmentStatus.RESCHEDULED));
        outboxRecorder.record(
                "Appointment",
                id,
                new AppointmentRescheduled(id, previousScheduledAt, scheduledAt, "RESCHEDULE"),
                correlationId(),
                id);
        return updated;
    }

    @Override
    @Transactional
    public Appointment validatePayment(String id) {
        Appointment existing = consult(id);
        PaymentAdapter.ValidationResult validation = paymentAdapter.validate(id);
        if (!validation.paid()) {
            throw new DomainException(ErrorCode.PAYMENT_NOT_CONFIRMED, "Payment not confirmed for: " + id);
        }
        if ("CONFIRMED".equalsIgnoreCase(existing.paymentStatus())) {
            return existing;
        }
        Appointment updated = appointmentRepository.save(existing.withPaymentStatus("CONFIRMED"));
        outboxRecorder.record(
                "Appointment",
                id,
                new PaymentConfirmed(
                        id,
                        validation.transactionId(),
                        0L,
                        "CLP",
                        "EXTERNAL",
                        Instant.now()),
                correlationId(),
                id);
        return updated;
    }

    @Override
    public MpiPatient mpiLookup(String appointmentId) {
        Appointment appointment = consult(appointmentId);
        MpiAdapter.PatientRecord record = mpiAdapter.lookup(appointment.patientId());
        return new MpiPatient(
                appointmentId, record.patientRef(), record.name(), record.dateOfBirth());
    }

    private static String stringOrDefault(Map<String, Object> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        return Instant.parse(value);
    }

    private static String correlationId() {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        return correlationId != null ? correlationId : "integration-service";
    }
}
