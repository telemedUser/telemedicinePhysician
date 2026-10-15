package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * The patient submitted a satisfaction survey.
 *
 * <p>The score is a service-quality rating rather than clinical information, so it is published;
 * any free-text comment stays in the survey service.
 *
 * @param appointmentId appointment that was surveyed
 * @param surveyId identifier of the survey instance
 * @param score numeric satisfaction score
 * @param completedAt when the response was submitted
 */
public record SurveyCompleted(String appointmentId, String surveyId, int score, Instant completedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.SURVEY_COMPLETED;
    }
}
