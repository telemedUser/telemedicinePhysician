package com.telemedicine.telemetryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.TelemetryMeasured;
import com.telemedicine.common.integration.ExternalSystems;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.telemetryservice.adapter.AnuraAdapter;
import com.telemedicine.telemetryservice.adapter.AnuraResults;
import com.telemedicine.telemetryservice.adapter.AnuraToken;
import com.telemedicine.telemetryservice.domain.MeasurementEntity;
import com.telemedicine.telemetryservice.repository.MeasurementRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryService {

    private static final Map<String, String> METRIC_KEYS =
            Map.of("heartRate", "HEART_RATE", "stress", "STRESS");

    private final AnuraAdapter anuraAdapter;
    private final MeasurementRepository measurementRepository;
    private final OutboxRecorder outboxRecorder;
    private final TelemetryActivationService activationService;
    private final ObjectMapper objectMapper;

    public TelemetryService(
            AnuraAdapter anuraAdapter,
            MeasurementRepository measurementRepository,
            OutboxRecorder outboxRecorder,
            TelemetryActivationService activationService,
            ObjectMapper objectMapper) {
        this.anuraAdapter = anuraAdapter;
        this.measurementRepository = measurementRepository;
        this.outboxRecorder = outboxRecorder;
        this.activationService = activationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, String> requestToken(
            String appointmentId, String sessionId, String appointmentType, String specialty) {
        activationService.requireEnabled(appointmentType, specialty);
        AnuraToken token = anuraAdapter.requestToken(appointmentId);
        return Map.of(
                "token", token.token(),
                "appointmentId", appointmentId,
                "sessionId", sessionId != null ? sessionId : "",
                "provider", ExternalSystems.ANURA);
    }

    @Transactional
    public Map<String, Object> submitResults(
            String appointmentId,
            String sessionId,
            String token,
            String appointmentType,
            String specialty) {
        activationService.requireEnabled(appointmentType, specialty);
        AnuraResults results = anuraAdapter.decryptResults(appointmentId, token);
        Instant measuredAt = Instant.now();
        List<Map<String, Object>> persisted = new ArrayList<>();

        for (Map.Entry<String, String> mapping : METRIC_KEYS.entrySet()) {
            Object rawValue = results.metrics().get(mapping.getKey());
            if (rawValue == null) {
                continue;
            }
            double value = toDouble(rawValue);
            MeasurementEntity measurement = persistMeasurement(
                    appointmentId, sessionId, mapping.getValue(), value, results, measuredAt);
            persisted.add(
                    Map.of(
                            "measurementId", measurement.getId(),
                            "metricType", measurement.getMetricType(),
                            "value", measurement.getValue()));
        }

        return Map.of(
                "appointmentId", appointmentId,
                "sessionId", sessionId != null ? sessionId : "",
                "measurements", persisted);
    }

    private MeasurementEntity persistMeasurement(
            String appointmentId,
            String sessionId,
            String metricType,
            double value,
            AnuraResults results,
            Instant measuredAt) {
        MeasurementEntity measurement = new MeasurementEntity();
        measurement.setId(IdGenerator.newUlid());
        measurement.setAppointmentId(appointmentId);
        measurement.setSessionId(sessionId);
        measurement.setMetricType(metricType);
        measurement.setProvider(ExternalSystems.ANURA);
        measurement.setValue(value);
        measurement.setMeasuredAt(measuredAt);
        try {
            measurement.setRawPayload(objectMapper.writeValueAsString(results.metrics()));
        } catch (Exception exception) {
            measurement.setRawPayload("{}");
        }
        measurementRepository.save(measurement);

        outboxRecorder.record(
                "Measurement",
                measurement.getId(),
                new TelemetryMeasured(
                        appointmentId,
                        sessionId,
                        measurement.getId(),
                        metricType,
                        ExternalSystems.ANURA,
                        measuredAt),
                correlationId(),
                appointmentId);
        return measurement;
    }

    private static double toDouble(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(rawValue));
    }

    private static String correlationId() {
        String fromMdc = MDC.get("correlationId");
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }
}
