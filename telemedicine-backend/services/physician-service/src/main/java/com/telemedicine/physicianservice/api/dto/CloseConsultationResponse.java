package com.telemedicine.physicianservice.api.dto;

import java.util.List;

/**
 * Result of a closure attempt.
 *
 * @param appointmentId the consultation
 * @param closed whether the consultation was closed and {@code ConsultationClosed} emitted
 * @param minimumSatisfied whether the minimum completion set was met
 * @param missingDocuments documents still required when the minimum was not met and no exception
 *     was recorded (TM-DOC-009)
 */
public record CloseConsultationResponse(
        String appointmentId,
        boolean closed,
        boolean minimumSatisfied,
        List<String> missingDocuments) {}
