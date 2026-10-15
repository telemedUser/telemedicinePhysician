package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The consultation was closed and written back to the source system.
 *
 * <p>This is the event the completed-consultation-rate KPI counts, and the trigger for the
 * satisfaction survey.
 *
 * @param appointmentId appointment that closed
 * @param outcome coded outcome, for example {@code COMPLETED} or {@code NO_SHOW}
 * @param durationSeconds total consultation duration
 * @param closedAt when closure was recorded
 */
public record ConsultationClosed(
        String appointmentId, String outcome, long durationSeconds, Instant closedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CONSULTATION_CLOSED;
    }
}
