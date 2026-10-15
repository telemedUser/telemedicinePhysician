package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A satisfaction survey was sent to the patient.
 *
 * @param appointmentId appointment being surveyed
 * @param surveyId identifier of the survey instance
 * @param channel delivery channel, for example {@code EMAIL}
 * @param requestedAt when the survey was sent
 */
public record SurveyRequested(
        String appointmentId, String surveyId, String channel, Instant requestedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.SURVEY_REQUESTED;
    }
}
