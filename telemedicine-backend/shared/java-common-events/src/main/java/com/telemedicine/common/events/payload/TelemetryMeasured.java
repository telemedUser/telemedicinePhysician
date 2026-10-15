package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A vital-sign measurement was captured, for example by ANURA.
 *
 * <p>The reading itself is clinical data and stays in the telemetry service behind encryption and
 * access control. This event announces that a measurement exists and where to find it.
 *
 * @param appointmentId appointment the measurement belongs to
 * @param sessionId session during which it was taken
 * @param measurementId identifier of the stored measurement
 * @param metricType coded metric, for example {@code HEART_RATE}
 * @param provider measuring provider, for example {@code anura}
 * @param measuredAt when the measurement was taken
 */
public record TelemetryMeasured(
        String appointmentId,
        String sessionId,
        String measurementId,
        String metricType,
        String provider,
        Instant measuredAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.TELEMETRY_MEASURED;
    }
}
