package com.telemedicine.waitingroomservice.queue;

import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.PatientEnteredWaitingRoom;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.devicetest.DeviceTestService;
import com.telemedicine.waitingroomservice.domain.QueueEntryEntity;
import com.telemedicine.waitingroomservice.domain.WaitingRoomStatus;
import com.telemedicine.waitingroomservice.repository.QueueEntryRepository;
import com.telemedicine.waitingroomservice.status.StatusService;
import com.telemedicine.waitingroomservice.web.dto.QueuePositionResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final RedisQueueStore redisQueueStore;
    private final DeviceTestService deviceTestService;
    private final StatusService statusService;
    private final OutboxRecorder outboxRecorder;
    private final SecurityContextAccessor securityContext;
    private final WaitingRoomServiceProperties properties;

    public QueueService(
            QueueEntryRepository queueEntryRepository,
            RedisQueueStore redisQueueStore,
            DeviceTestService deviceTestService,
            StatusService statusService,
            OutboxRecorder outboxRecorder,
            SecurityContextAccessor securityContext,
            WaitingRoomServiceProperties properties) {
        this.queueEntryRepository = queueEntryRepository;
        this.redisQueueStore = redisQueueStore;
        this.deviceTestService = deviceTestService;
        this.statusService = statusService;
        this.outboxRecorder = outboxRecorder;
        this.securityContext = securityContext;
        this.properties = properties;
    }

    @Transactional
    public QueuePositionResponse position(String appointmentId) {
        deviceTestService.requirePassed(appointmentId);
        var status = statusService.loadOrCreate(appointmentId);
        if (status.statusEnum() != WaitingRoomStatus.CHECKED_IN
                && status.statusEnum() != WaitingRoomStatus.OPEN_DOCTOR) {
            throw new ConflictException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "Patient must be checked in before entering the queue");
        }

        QueueEntryEntity entry =
                queueEntryRepository
                        .findByAppointmentId(appointmentId)
                        .orElseGet(() -> enterQueue(appointmentId));

        int position = redisQueueStore.position(appointmentId);
        if (position <= 0) {
            position = entry.getQueuePosition();
        }
        int estimatedWaitSec = Math.max(0, (position - 1) * properties.getQueueAvgWaitSec());
        return new QueuePositionResponse(
                appointmentId, position, estimatedWaitSec, "PatientEnteredWaitingRoom");
    }

    private QueueEntryEntity enterQueue(String appointmentId) {
        String patientRef =
                securityContext
                        .currentPatientId()
                        .orElseGet(() -> securityContext.currentUserId().orElse(appointmentId));
        Instant enteredAt = Instant.now();
        int position = (int) redisQueueStore.size() + 1;

        QueueEntryEntity entity = new QueueEntryEntity();
        entity.setId(IdGenerator.newId());
        entity.setAppointmentId(appointmentId);
        entity.setPatientRef(patientRef);
        entity.setQueuePosition(position);
        entity.setEnteredAt(enteredAt);
        entity.setCreatedAt(enteredAt);
        queueEntryRepository.save(entity);
        redisQueueStore.add(appointmentId, enteredAt);

        PatientEnteredWaitingRoom payload =
                new PatientEnteredWaitingRoom(appointmentId, patientRef, position, enteredAt);
        outboxRecorder.record(
                "queue",
                appointmentId,
                payload,
                IdGenerator.correlationId(),
                appointmentId);
        return entity;
    }
}
