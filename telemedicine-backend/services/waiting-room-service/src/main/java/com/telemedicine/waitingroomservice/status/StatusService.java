package com.telemedicine.waitingroomservice.status;

import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IdentityValidationException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.AppointmentCreated;
import com.telemedicine.common.events.payload.AppointmentRescheduled;
import com.telemedicine.common.events.payload.StatusChanged;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.consent.ConsentService;
import com.telemedicine.waitingroomservice.devicetest.DeviceTestService;
import com.telemedicine.waitingroomservice.domain.AppointmentStatusEntity;
import com.telemedicine.waitingroomservice.domain.WaitingRoomStatus;
import com.telemedicine.waitingroomservice.integration.IntegrationGateway;
import com.telemedicine.waitingroomservice.repository.AppointmentStatusRepository;
import com.telemedicine.waitingroomservice.web.dto.StatusUpdateRequest;
import com.telemedicine.waitingroomservice.web.dto.StatusUpdateResponse;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusService {

    private final AppointmentStatusRepository appointmentStatusRepository;
    private final ConsentService consentService;
    private final DeviceTestService deviceTestService;
    private final IntegrationGateway integrationGateway;
    private final OutboxRecorder outboxRecorder;
    private final SecurityContextAccessor securityContext;
    private final JourneyMetrics journeyMetrics;
    private final WaitingRoomServiceProperties properties;
    private final Map<String, Timer.Sample> waitingSamples = new ConcurrentHashMap<>();

    public StatusService(
            AppointmentStatusRepository appointmentStatusRepository,
            ConsentService consentService,
            DeviceTestService deviceTestService,
            IntegrationGateway integrationGateway,
            OutboxRecorder outboxRecorder,
            SecurityContextAccessor securityContext,
            JourneyMetrics journeyMetrics,
            WaitingRoomServiceProperties properties) {
        this.appointmentStatusRepository = appointmentStatusRepository;
        this.consentService = consentService;
        this.deviceTestService = deviceTestService;
        this.integrationGateway = integrationGateway;
        this.outboxRecorder = outboxRecorder;
        this.securityContext = securityContext;
        this.journeyMetrics = journeyMetrics;
        this.properties = properties;
    }

    @Transactional
    public StatusUpdateResponse update(String appointmentId, StatusUpdateRequest request) {
        WaitingRoomStatus target = WaitingRoomStatus.fromWire(request.status());
        AppointmentStatusEntity entity = loadOrCreate(appointmentId);
        WaitingRoomStatus current = entity.statusEnum();

        if (current == target) {
            return new StatusUpdateResponse(appointmentId, current.name(), "StatusChanged");
        }

        validateTransition(current, target);
        if (target == WaitingRoomStatus.CHECKED_IN) {
            enforceCheckInGates(entity);
        }

        Instant now = Instant.now();
        if (target == WaitingRoomStatus.CHECKED_IN) {
            entity.setCheckedInAt(now);
            waitingSamples.put(appointmentId, journeyMetrics.startWaitingTimeSample());
        }
        if (target == WaitingRoomStatus.OPEN_DOCTOR) {
            entity.setOpenDoctorAt(now);
            Timer.Sample sample = waitingSamples.remove(appointmentId);
            if (sample != null) {
                journeyMetrics.recordWaitingTime(sample, appointmentId);
            }
            integrationGateway.syncHisStatus(appointmentId, target.name());
        }

        entity.setStatusEnum(target);
        entity.setUpdatedAt(now);
        appointmentStatusRepository.save(entity);

        publishStatusChanged(appointmentId, current, target, now);
        return new StatusUpdateResponse(appointmentId, target.name(), "StatusChanged");
    }

    public AppointmentStatusEntity loadOrCreate(String appointmentId) {
        return appointmentStatusRepository
                .findById(appointmentId)
                .orElseGet(() -> createInitial(appointmentId));
    }

    @Transactional
    public void initializeFromAppointmentCreated(AppointmentCreated created) {
        AppointmentStatusEntity entity = loadOrCreate(created.appointmentId());
        if (entity.getPatientRef() == null && created.patientRef() != null) {
            entity.setPatientRef(created.patientRef());
        }
        entity.setUpdatedAt(Instant.now());
        appointmentStatusRepository.save(entity);
    }

    @Transactional
    public void handleAppointmentRescheduled(AppointmentRescheduled rescheduled) {
        AppointmentStatusEntity entity = loadOrCreate(rescheduled.appointmentId());
        entity.setUpdatedAt(rescheduled.scheduledAt());
        appointmentStatusRepository.save(entity);
    }

    @Transactional
    public void markIdentityValidated(String appointmentId, String patientRef) {
        AppointmentStatusEntity entity = loadOrCreate(appointmentId);
        entity.setIdentityValidated(true);
        if (entity.getPatientRef() == null) {
            entity.setPatientRef(patientRef);
        }
        entity.setUpdatedAt(Instant.now());
        appointmentStatusRepository.save(entity);
    }

    @Transactional
    public int expireTimedOutPatients(Instant now) {
        Instant cutoff = now.minus(properties.getWaitingRoomTimeout());
        var candidates =
                appointmentStatusRepository.findByStatusAndCheckedInAtBefore(
                        WaitingRoomStatus.CHECKED_IN.name(), cutoff);
        int processed = 0;
        for (AppointmentStatusEntity entity : candidates) {
            if (entity.getCheckedInAt() == null) {
                continue;
            }
            WaitingRoomStatus from = entity.statusEnum();
            entity.setStatusEnum(WaitingRoomStatus.TIMED_OUT);
            entity.setTimedOutAt(now);
            entity.setUpdatedAt(now);
            appointmentStatusRepository.save(entity);
            publishStatusChanged(entity.getAppointmentId(), from, WaitingRoomStatus.TIMED_OUT, now);
            journeyMetrics.recordConsultationAbandoned(entity.getAppointmentId(), "WAITING_TIMEOUT");
            processed++;
        }
        return processed;
    }

    @Transactional
    public int detectNoShows(Instant now, java.time.Duration threshold) {
        Instant cutoff = now.minus(threshold);
        var candidates =
                appointmentStatusRepository.findByStatusAndUpdatedAtBefore(
                        WaitingRoomStatus.OPEN_PATIENT.name(), cutoff);
        int processed = 0;
        for (AppointmentStatusEntity entity : candidates) {
            WaitingRoomStatus from = entity.statusEnum();
            entity.setStatusEnum(WaitingRoomStatus.NO_SHOW);
            entity.setNoShowAt(now);
            entity.setUpdatedAt(now);
            appointmentStatusRepository.save(entity);
            publishStatusChanged(entity.getAppointmentId(), from, WaitingRoomStatus.NO_SHOW, now);
            journeyMetrics.recordConsultationAbandoned(entity.getAppointmentId(), "NO_SHOW");
            processed++;
        }
        return processed;
    }

    private AppointmentStatusEntity createInitial(String appointmentId) {
        AppointmentStatusEntity entity = new AppointmentStatusEntity();
        entity.setAppointmentId(appointmentId);
        entity.setStatusEnum(WaitingRoomStatus.OPEN_PATIENT);
        entity.setUpdatedAt(Instant.now());
        String patientRef =
                securityContext
                        .currentPatientId()
                        .orElseGet(() -> securityContext.currentUserId().orElse(null));
        entity.setPatientRef(patientRef);
        return appointmentStatusRepository.save(entity);
    }

    private void validateTransition(WaitingRoomStatus current, WaitingRoomStatus target) {
        boolean allowed =
                switch (current) {
                    case OPEN_PATIENT -> target == WaitingRoomStatus.CHECKED_IN;
                    case CHECKED_IN -> target == WaitingRoomStatus.OPEN_DOCTOR;
                    default -> false;
                };
        if (!allowed) {
            throw ConflictException.invalidTransition(current.name(), target.name());
        }
    }

    private void enforceCheckInGates(AppointmentStatusEntity entity) {
        String subjectId =
                entity.getPatientRef() != null
                        ? entity.getPatientRef()
                        : securityContext
                                .currentPatientId()
                                .orElseGet(
                                        () ->
                                                securityContext
                                                        .currentUserId()
                                                        .orElse(entity.getAppointmentId()));
        consentService.requireBothConsents(subjectId);
        if (!entity.isIdentityValidated()) {
            throw new IdentityValidationException(
                    ErrorCode.IDENTITY_VALIDATION_FAILED,
                    "Identity must be validated before check-in");
        }
        deviceTestService.requirePassed(entity.getAppointmentId());
        if (!entity.isPaymentConfirmed()) {
            if (integrationGateway.isPaymentConfirmed(entity.getAppointmentId())) {
                entity.setPaymentConfirmed(true);
            } else {
                throw new DomainException(
                        ErrorCode.PAYMENT_NOT_CONFIRMED,
                        "Payment must be confirmed before check-in");
            }
        }
    }

    private void publishStatusChanged(
            String appointmentId, WaitingRoomStatus from, WaitingRoomStatus to, Instant changedAt) {
        String actorRef =
                securityContext
                        .currentUserId()
                        .orElseGet(() -> securityContext.currentUsername().orElse("system"));
        StatusChanged payload =
                new StatusChanged(appointmentId, from.name(), to.name(), actorRef, changedAt);
        outboxRecorder.record(
                "appointment-status",
                appointmentId,
                payload,
                IdGenerator.correlationId(),
                appointmentId);
    }
}
