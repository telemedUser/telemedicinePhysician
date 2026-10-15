package com.telemedicine.common.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Pre-named journey KPI metrics referenced by the design document so reporting-service can
 * aggregate them without inventing new names.
 */
public class JourneyMetrics {

    public static final String METRIC_WAITING_TIME = "telemedicine.journey.waiting.time";
    public static final String METRIC_VIDEO_ENTER_TIME = "telemedicine.journey.video.enter.time";
    public static final String METRIC_CONSULTATION_COMPLETED =
            "telemedicine.journey.consultation.completed";
    public static final String METRIC_CONSULTATION_ABANDONED =
            "telemedicine.journey.consultation.abandoned";
    public static final String METRIC_DOCUMENT_ISSUANCE_TIME =
            "telemedicine.journey.document.issuance.time";

    private final MeterRegistry registry;

    public JourneyMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startWaitingTimeSample() {
        return Timer.start(registry);
    }

    public void recordWaitingTime(Timer.Sample sample, String appointmentId) {
        sample.stop(
                Timer.builder(METRIC_WAITING_TIME)
                        .description("Elapsed time from check-in to physician admission")
                        .tag("appointmentId", safeTag(appointmentId))
                        .register(registry));
    }

    public Timer.Sample startVideoEnterSample() {
        return Timer.start(registry);
    }

    public void recordVideoEnterTime(Timer.Sample sample, String appointmentId) {
        sample.stop(
                Timer.builder(METRIC_VIDEO_ENTER_TIME)
                        .description("Elapsed time from link opened to video session connected")
                        .tag("appointmentId", safeTag(appointmentId))
                        .register(registry));
    }

    public void recordConsultationCompleted(String appointmentId) {
        Counter.builder(METRIC_CONSULTATION_COMPLETED)
                .description("Consultations completed successfully")
                .tag("appointmentId", safeTag(appointmentId))
                .register(registry)
                .increment();
    }

    public void recordConsultationAbandoned(String appointmentId, String reason) {
        Counter.builder(METRIC_CONSULTATION_ABANDONED)
                .description("Consultations abandoned before completion")
                .tag("appointmentId", safeTag(appointmentId))
                .tag("reason", safeTag(reason))
                .register(registry)
                .increment();
    }

    public void recordDocumentIssuanceTime(Duration duration, String documentType) {
        Timer.builder(METRIC_DOCUMENT_ISSUANCE_TIME)
                .description("Time from consultation end to document availability")
                .tag("documentType", safeTag(documentType))
                .register(registry)
                .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}
