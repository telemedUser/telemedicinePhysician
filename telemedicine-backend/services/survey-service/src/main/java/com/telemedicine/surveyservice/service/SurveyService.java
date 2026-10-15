package com.telemedicine.surveyservice.service;

import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.SurveyCompleted;
import com.telemedicine.common.events.payload.SurveyRequested;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.surveyservice.api.dto.SubmitSurveyResponseRequest;
import com.telemedicine.surveyservice.api.dto.SurveyAvailabilityResponse;
import com.telemedicine.surveyservice.config.SurveyServiceProperties;
import com.telemedicine.surveyservice.domain.SurveyDefinitionEntity;
import com.telemedicine.surveyservice.domain.SurveyRequestEntity;
import com.telemedicine.surveyservice.domain.SurveyResponseEntity;
import com.telemedicine.surveyservice.repository.SurveyDefinitionRepository;
import com.telemedicine.surveyservice.repository.SurveyRequestRepository;
import com.telemedicine.surveyservice.repository.SurveyResponseRepository;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveyService {

    public static final String SURVEY_NAME = "post-consultation-nps";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DISPATCHED = "DISPATCHED";

    private final SurveyDefinitionRepository definitionRepository;
    private final SurveyRequestRepository requestRepository;
    private final SurveyResponseRepository responseRepository;
    private final OutboxRecorder outboxRecorder;
    private final SurveyServiceProperties properties;

    public SurveyService(
            SurveyDefinitionRepository definitionRepository,
            SurveyRequestRepository requestRepository,
            SurveyResponseRepository responseRepository,
            OutboxRecorder outboxRecorder,
            SurveyServiceProperties properties) {
        this.definitionRepository = definitionRepository;
        this.requestRepository = requestRepository;
        this.responseRepository = responseRepository;
        this.outboxRecorder = outboxRecorder;
        this.properties = properties;
    }

    @Transactional
    public SurveyRequestEntity requestSurveyForAppointment(String appointmentId) {
        return requestRepository
                .findByAppointmentId(appointmentId)
                .orElseGet(() -> createSurveyRequest(appointmentId));
    }

    private SurveyRequestEntity createSurveyRequest(String appointmentId) {
        SurveyDefinitionEntity definition =
                definitionRepository
                        .findFirstByNameAndActiveTrueOrderBySurveyVersionDesc(SURVEY_NAME)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.SURVEY_NOT_FOUND, "post-consultation-nps"));

        SurveyRequestEntity request = new SurveyRequestEntity();
        request.setId(IdGenerator.newUlid());
        request.setAppointmentId(appointmentId);
        request.setSurveyDefinitionId(definition.getId());
        request.setChannel(properties.getDefaultChannel());
        request.setStatus(STATUS_PENDING);
        request.setRequestedAt(Instant.now());
        requestRepository.save(request);

        publishSurveyRequested(request);
        request.setStatus(STATUS_DISPATCHED);
        return requestRepository.save(request);
    }

    @Transactional
    public int redispatchPendingSurveys(Instant cutoff) {
        int processed = 0;
        for (SurveyRequestEntity request :
                requestRepository.findByStatusAndRequestedAtBefore(STATUS_PENDING, cutoff)) {
            publishSurveyRequested(request);
            request.setStatus(STATUS_DISPATCHED);
            requestRepository.save(request);
            processed++;
        }
        return processed;
    }

    @Transactional
    public SurveyAvailabilityResponse availability(String appointmentId) {
        boolean submitted = responseRepository.existsByAppointmentId(appointmentId);
        boolean available =
                requestRepository.findByAppointmentId(appointmentId).isPresent() && !submitted;
        return new SurveyAvailabilityResponse(appointmentId, available, submitted);
    }

    @Transactional
    public SurveyAvailabilityResponse submitResponse(
            String appointmentId, SubmitSurveyResponseRequest request) {
        if (responseRepository.existsByAppointmentId(appointmentId)) {
            throw new ConflictException(ErrorCode.SURVEY_ALREADY_SUBMITTED, appointmentId);
        }
        SurveyRequestEntity surveyRequest =
                requestRepository
                        .findByAppointmentId(appointmentId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.SURVEY_NOT_FOUND, appointmentId));

        SurveyResponseEntity response = new SurveyResponseEntity();
        response.setId(IdGenerator.newUlid());
        response.setSurveyRequestId(surveyRequest.getId());
        response.setAppointmentId(appointmentId);
        response.setScore(request.nps());
        response.setComment(request.comment());
        response.setCompletedAt(Instant.now());
        responseRepository.save(response);

        outboxRecorder.record(
                "SurveyResponse",
                response.getId(),
                new SurveyCompleted(
                        appointmentId, surveyRequest.getId(), request.nps(), response.getCompletedAt()),
                correlationId(),
                appointmentId);

        return new SurveyAvailabilityResponse(appointmentId, false, true);
    }

    private void publishSurveyRequested(SurveyRequestEntity request) {
        outboxRecorder.record(
                "SurveyRequest",
                request.getId(),
                new SurveyRequested(
                        request.getAppointmentId(),
                        request.getId(),
                        request.getChannel(),
                        request.getRequestedAt()),
                correlationId(),
                request.getAppointmentId());
    }

    private static String correlationId() {
        String fromMdc = MDC.get("correlationId");
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }
}
