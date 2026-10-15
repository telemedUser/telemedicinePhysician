package com.telemedicine.waitingroomservice.devicetest;

import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.DeviceTestCompleted;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.waitingroomservice.domain.DeviceTestEntity;
import com.telemedicine.waitingroomservice.repository.DeviceTestRepository;
import com.telemedicine.waitingroomservice.web.dto.DeviceTestRequest;
import com.telemedicine.waitingroomservice.web.dto.DeviceTestResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceTestService {

    private static final int MIN_BANDWIDTH_KBPS = 512;

    private final DeviceTestRepository deviceTestRepository;
    private final OutboxRecorder outboxRecorder;
    private final SecurityContextAccessor securityContext;

    public DeviceTestService(
            DeviceTestRepository deviceTestRepository,
            OutboxRecorder outboxRecorder,
            SecurityContextAccessor securityContext) {
        this.deviceTestRepository = deviceTestRepository;
        this.outboxRecorder = outboxRecorder;
        this.securityContext = securityContext;
    }

    @Transactional
    public DeviceTestResponse submit(String appointmentId, DeviceTestRequest request) {
        boolean cameraOk = request.camera() == null || request.camera();
        boolean microphoneOk = request.microphone() == null || request.microphone();
        int bandwidthKbps =
                request.networkMbps() == null ? MIN_BANDWIDTH_KBPS : request.networkMbps() * 1000;
        boolean passed = cameraOk && microphoneOk && bandwidthKbps >= MIN_BANDWIDTH_KBPS;

        if (!passed) {
            throw new ConflictException(ErrorCode.DEVICE_TEST_REQUIRED, "Device test did not pass");
        }

        String patientRef =
                securityContext
                        .currentPatientId()
                        .orElseGet(() -> securityContext.currentUserId().orElse(appointmentId));
        Instant completedAt = Instant.now();

        DeviceTestEntity entity =
                deviceTestRepository
                        .findByAppointmentId(appointmentId)
                        .orElseGet(DeviceTestEntity::new);
        if (entity.getId() == null) {
            entity.setId(IdGenerator.newId());
            entity.setCreatedAt(completedAt);
        }
        entity.setAppointmentId(appointmentId);
        entity.setPatientRef(patientRef);
        entity.setCameraOk(cameraOk);
        entity.setMicrophoneOk(microphoneOk);
        entity.setBandwidthKbps(bandwidthKbps);
        entity.setPassed(true);
        entity.setCompletedAt(completedAt);
        deviceTestRepository.save(entity);

        DeviceTestCompleted payload =
                new DeviceTestCompleted(
                        appointmentId, patientRef, cameraOk, microphoneOk, bandwidthKbps, completedAt);
        outboxRecorder.record(
                "device-test",
                appointmentId,
                payload,
                IdGenerator.correlationId(),
                appointmentId);

        return new DeviceTestResponse(
                appointmentId, cameraOk, microphoneOk, bandwidthKbps / 1000, "DeviceTestCompleted");
    }

    public void requirePassed(String appointmentId) {
        deviceTestRepository
                .findByAppointmentId(appointmentId)
                .filter(DeviceTestEntity::isPassed)
                .orElseThrow(
                        () ->
                                new ConflictException(
                                        ErrorCode.DEVICE_TEST_REQUIRED,
                                        "Device test must pass before proceeding"));
    }
}
