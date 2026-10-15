package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The patient finished the camera, microphone and bandwidth check.
 *
 * @param appointmentId appointment the check belongs to
 * @param patientRef pseudonymous patient reference
 * @param cameraOk whether the camera was usable
 * @param microphoneOk whether the microphone was usable
 * @param bandwidthKbps measured downstream bandwidth
 * @param completedAt when the check finished
 */
public record DeviceTestCompleted(
        String appointmentId,
        String patientRef,
        boolean cameraOk,
        boolean microphoneOk,
        int bandwidthKbps,
        Instant completedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.DEVICE_TEST_COMPLETED;
    }
}
