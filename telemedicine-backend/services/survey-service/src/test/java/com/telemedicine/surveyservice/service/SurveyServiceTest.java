package com.telemedicine.surveyservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.events.payload.SurveyCompleted;
import com.telemedicine.common.events.payload.SurveyRequested;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.surveyservice.api.dto.SubmitSurveyResponseRequest;
import com.telemedicine.surveyservice.config.SurveyServiceProperties;
import com.telemedicine.surveyservice.domain.SurveyDefinitionEntity;
import com.telemedicine.surveyservice.domain.SurveyRequestEntity;
import com.telemedicine.surveyservice.repository.SurveyDefinitionRepository;
import com.telemedicine.surveyservice.repository.SurveyRequestRepository;
import com.telemedicine.surveyservice.repository.SurveyResponseRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock private SurveyDefinitionRepository definitionRepository;
    @Mock private SurveyRequestRepository requestRepository;
    @Mock private SurveyResponseRepository responseRepository;
    @Mock private OutboxRecorder outboxRecorder;

    private SurveyService surveyService;

    @BeforeEach
    void setUp() {
        SurveyServiceProperties properties = new SurveyServiceProperties();
        properties.setDefaultChannel("email");
        properties.setDispatchBackupMinutes(Duration.ofMinutes(15));
        surveyService =
                new SurveyService(
                        definitionRepository,
                        requestRepository,
                        responseRepository,
                        outboxRecorder,
                        properties);
    }

    @Test
    void requestSurveyEmitsSurveyRequested() {
        when(requestRepository.findByAppointmentId("apt-1")).thenReturn(Optional.empty());
        SurveyDefinitionEntity definition = new SurveyDefinitionEntity();
        definition.setId("nps-v1");
        definition.setName(SurveyService.SURVEY_NAME);
        definition.setActive(true);
        when(definitionRepository.findFirstByNameAndActiveTrueOrderBySurveyVersionDesc(SurveyService.SURVEY_NAME))
                .thenReturn(Optional.of(definition));
        when(requestRepository.save(any(SurveyRequestEntity.class)))
                .thenAnswer(
                        invocation -> {
                            SurveyRequestEntity entity = invocation.getArgument(0);
                            if (entity.getId() == null) {
                                entity.setId("req-1");
                            }
                            return entity;
                        });

        surveyService.requestSurveyForAppointment("apt-1");

        verify(outboxRecorder).record(eq("SurveyRequest"), any(), any(SurveyRequested.class), any(), eq("apt-1"));
    }

    @Test
    void duplicateSubmissionFailsWithConflict() {
        when(responseRepository.existsByAppointmentId("apt-1")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                surveyService.submitResponse(
                                        "apt-1", new SubmitSurveyResponseRequest(9, null)))
                .isInstanceOfSatisfying(
                        ConflictException.class,
                        exception ->
                                assertThat(exception.getCode())
                                        .isEqualTo(ErrorCode.SURVEY_ALREADY_SUBMITTED));
    }

    @Test
    void submitResponseEmitsSurveyCompleted() {
        when(responseRepository.existsByAppointmentId("apt-1")).thenReturn(false);
        SurveyRequestEntity request = new SurveyRequestEntity();
        request.setId("req-1");
        request.setAppointmentId("apt-1");
        when(requestRepository.findByAppointmentId("apt-1")).thenReturn(Optional.of(request));

        var response = surveyService.submitResponse("apt-1", new SubmitSurveyResponseRequest(10, "Great"));

        assertThat(response.submitted()).isTrue();
        verify(responseRepository).save(any());
        verify(outboxRecorder)
                .record(eq("SurveyResponse"), any(), any(SurveyCompleted.class), any(), eq("apt-1"));
    }

    @Test
    void availabilityReflectsPendingSurvey() {
        when(responseRepository.existsByAppointmentId("apt-1")).thenReturn(false);
        SurveyRequestEntity request = new SurveyRequestEntity();
        request.setAppointmentId("apt-1");
        when(requestRepository.findByAppointmentId("apt-1")).thenReturn(Optional.of(request));

        var response = surveyService.availability("apt-1");

        assertThat(response.available()).isTrue();
        assertThat(response.submitted()).isFalse();
    }

    @Test
    void idempotentSurveyRequestDoesNotDuplicate() {
        SurveyRequestEntity existing = new SurveyRequestEntity();
        existing.setId("req-1");
        when(requestRepository.findByAppointmentId("apt-1")).thenReturn(Optional.of(existing));

        surveyService.requestSurveyForAppointment("apt-1");

        verify(outboxRecorder, never()).record(any(), any(), any(SurveyRequested.class), any());
    }
}
