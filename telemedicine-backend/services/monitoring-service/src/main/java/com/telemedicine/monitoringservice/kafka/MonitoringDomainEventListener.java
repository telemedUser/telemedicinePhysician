package com.telemedicine.monitoringservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.ConnectionQualityDegraded;
import com.telemedicine.common.events.payload.DeviceTestCompleted;
import com.telemedicine.common.events.payload.IdentityValidationFailed;
import com.telemedicine.common.events.payload.IntegrationFailed;
import com.telemedicine.common.events.payload.NotificationFailed;
import com.telemedicine.common.events.payload.PatientEnteredWaitingRoom;
import com.telemedicine.common.events.payload.StatusChanged;
import com.telemedicine.common.events.payload.TelemetryMeasured;
import com.telemedicine.common.events.payload.VideoSessionCreated;
import com.telemedicine.common.events.payload.VideoSessionEnded;
import com.telemedicine.common.events.payload.VideoSessionStarted;
import com.telemedicine.monitoringservice.service.MonitoringSnapshotService;
import com.telemedicine.monitoringservice.service.SessionHealthService;
import com.telemedicine.monitoringservice.service.SloService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class MonitoringDomainEventListener {

    private final ObjectMapper eventObjectMapper;
    private final SessionHealthService sessionHealthService;
    private final MonitoringSnapshotService snapshotService;
    private final SloService sloService;

    public MonitoringDomainEventListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            SessionHealthService sessionHealthService,
            MonitoringSnapshotService snapshotService,
            SloService sloService) {
        this.eventObjectMapper = eventObjectMapper;
        this.sessionHealthService = sessionHealthService;
        this.snapshotService = snapshotService;
        this.sloService = sloService;
    }

    @DomainEventListener(EventTypes.VIDEO_SESSION_CREATED)
    @IdempotentConsumer
    public void onVideoSessionCreated(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<VideoSessionCreated> envelope =
                readEnvelope(record.value(), VideoSessionCreated.class);
        VideoSessionCreated payload = envelope.payload();
        sloService.recordRequest();
        sessionHealthService.onSessionCreated(
                payload.sessionId(), payload.appointmentId(), payload.provider(), payload.createdAt());
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.VIDEO_SESSION_STARTED)
    @IdempotentConsumer
    public void onVideoSessionStarted(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<VideoSessionStarted> envelope =
                readEnvelope(record.value(), VideoSessionStarted.class);
        VideoSessionStarted payload = envelope.payload();
        sloService.recordRequest();
        sessionHealthService.onSessionStarted(
                payload.sessionId(), payload.appointmentId(), payload.provider(), payload.startedAt());
        snapshotService.decrementWaitingQueue();
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.VIDEO_SESSION_ENDED)
    @IdempotentConsumer
    public void onVideoSessionEnded(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<VideoSessionEnded> envelope = readEnvelope(record.value(), VideoSessionEnded.class);
        VideoSessionEnded payload = envelope.payload();
        sloService.recordRequest();
        sessionHealthService.onSessionEnded(payload.sessionId(), payload.endedAt());
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.TELEMETRY_MEASURED)
    @IdempotentConsumer
    public void onTelemetryMeasured(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        sloService.recordRequest();
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.STATUS_CHANGED)
    @IdempotentConsumer
    public void onStatusChanged(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<StatusChanged> envelope = readEnvelope(record.value(), StatusChanged.class);
        StatusChanged payload = envelope.payload();
        sloService.recordRequest();
        if ("OPEN_DOCTOR".equalsIgnoreCase(payload.toStatus())) {
            snapshotService.decrementWaitingQueue();
        }
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.PATIENT_ENTERED_WAITING_ROOM)
    @IdempotentConsumer
    public void onPatientEnteredWaitingRoom(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        sloService.recordRequest();
        snapshotService.incrementWaitingQueue();
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.INTEGRATION_FAILED)
    @IdempotentConsumer
    public void onIntegrationFailed(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        readEnvelope(record.value(), IntegrationFailed.class);
        sloService.recordError();
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.DEVICE_TEST_COMPLETED)
    @IdempotentConsumer
    public void onDeviceTestCompleted(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<DeviceTestCompleted> envelope =
                readEnvelope(record.value(), DeviceTestCompleted.class);
        DeviceTestCompleted payload = envelope.payload();
        sloService.recordRequest();
        if (!payload.cameraOk() || !payload.microphoneOk()) {
            sloService.recordError();
        }
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.IDENTITY_VALIDATION_FAILED)
    @IdempotentConsumer
    public void onIdentityValidationFailed(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<IdentityValidationFailed> envelope =
                readEnvelope(record.value(), IdentityValidationFailed.class);
        IdentityValidationFailed payload = envelope.payload();
        sloService.recordError();
        if (payload.attempt() >= 3) {
            snapshotService.refreshSnapshot();
        }
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.CONNECTION_QUALITY_DEGRADED)
    @IdempotentConsumer
    public void onConnectionQualityDegraded(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<ConnectionQualityDegraded> envelope =
                readEnvelope(record.value(), ConnectionQualityDegraded.class);
        ConnectionQualityDegraded payload = envelope.payload();
        sloService.recordRequest();
        if (payload.qualityScore() <= 2) {
            snapshotService.refreshSnapshot();
        }
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.NOTIFICATION_FAILED)
    @IdempotentConsumer
    public void onNotificationFailed(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        readEnvelope(record.value(), NotificationFailed.class);
        sloService.recordError();
        snapshotService.refreshSnapshot();
        acknowledgment.acknowledge();
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
