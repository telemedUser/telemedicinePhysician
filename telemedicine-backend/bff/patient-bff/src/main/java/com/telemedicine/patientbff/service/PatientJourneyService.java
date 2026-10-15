package com.telemedicine.patientbff.service;

import com.telemedicine.patientbff.client.DownstreamClient;
import com.telemedicine.patientbff.client.DownstreamClient.FetchResult;
import com.telemedicine.patientbff.dto.AggregationStatus;
import com.telemedicine.patientbff.dto.ConsentState;
import com.telemedicine.patientbff.dto.DocumentsState;
import com.telemedicine.patientbff.dto.IdentityState;
import com.telemedicine.patientbff.dto.PatientJourneyResponse;
import com.telemedicine.patientbff.dto.SurveyState;
import com.telemedicine.patientbff.dto.VideoState;
import com.telemedicine.patientbff.dto.WaitingRoomState;
import com.telemedicine.patientbff.dto.downstream.AppointmentSummary;
import com.telemedicine.patientbff.dto.downstream.ClinicalDocsSummary;
import com.telemedicine.patientbff.dto.downstream.IdentityStatus;
import com.telemedicine.patientbff.dto.downstream.QueuePosition;
import com.telemedicine.patientbff.dto.downstream.SurveyAvailability;
import com.telemedicine.patientbff.dto.downstream.VideoSessionSummary;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PatientJourneyService {

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<QueuePosition>>
            QUEUE_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<AppointmentSummary>>
            APPOINTMENT_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<ClinicalDocsSummary>>
            DOCS_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<Map<String, Object>>>
            CONSENT_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<IdentityStatus>>
            IDENTITY_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<VideoSessionSummary>>
            VIDEO_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<SurveyAvailability>>
            SURVEY_TYPE = new ParameterizedTypeReference<>() {};

    private final DownstreamClient downstreamClient;
    private final RestClient waitingRoomRestClient;
    private final RestClient integrationRestClient;
    private final RestClient clinicalDocsRestClient;
    private final RestClient identityRestClient;
    private final RestClient videoRestClient;
    private final RestClient surveyRestClient;

    public PatientJourneyService(
            DownstreamClient downstreamClient,
            @Qualifier("waitingRoomRestClient") RestClient waitingRoomRestClient,
            @Qualifier("integrationRestClient") RestClient integrationRestClient,
            @Qualifier("clinicalDocsRestClient") RestClient clinicalDocsRestClient,
            @Qualifier("identityRestClient") RestClient identityRestClient,
            @Qualifier("videoRestClient") RestClient videoRestClient,
            @Qualifier("surveyRestClient") RestClient surveyRestClient) {
        this.downstreamClient = downstreamClient;
        this.waitingRoomRestClient = waitingRoomRestClient;
        this.integrationRestClient = integrationRestClient;
        this.clinicalDocsRestClient = clinicalDocsRestClient;
        this.identityRestClient = identityRestClient;
        this.videoRestClient = videoRestClient;
        this.surveyRestClient = surveyRestClient;
    }

    public PatientJourneyResponse journey(String appointmentId) {
        FetchResult<QueuePosition> queue =
                downstreamClient.get(
                        waitingRoomRestClient,
                        "/api/v1/waiting-room/queue/{appointmentId}",
                        new Object[] {appointmentId},
                        QUEUE_TYPE);
        FetchResult<AppointmentSummary> appointment =
                downstreamClient.get(
                        integrationRestClient,
                        "/v1/appointments/v2/{appointmentId}",
                        new Object[] {appointmentId},
                        APPOINTMENT_TYPE);
        FetchResult<ClinicalDocsSummary> documents =
                downstreamClient.get(
                        clinicalDocsRestClient,
                        "/api/v1/clinical-docs/{appointmentId}",
                        new Object[] {appointmentId},
                        DOCS_TYPE);
        FetchResult<Map<String, Object>> platformConsent =
                downstreamClient.get(
                        waitingRoomRestClient,
                        "/api/v1/waiting-room/consent/{subjectId}?type={type}",
                        new Object[] {appointmentId, "PLATFORM_TERMS"},
                        CONSENT_TYPE);
        FetchResult<Map<String, Object>> clinicalConsent =
                downstreamClient.get(
                        waitingRoomRestClient,
                        "/api/v1/waiting-room/consent/{subjectId}?type={type}",
                        new Object[] {appointmentId, "CLINICAL_INFORMED"},
                        CONSENT_TYPE);
        FetchResult<IdentityStatus> identity =
                downstreamClient.get(
                        identityRestClient,
                        "/api/v1/identity/{appointmentId}/status",
                        new Object[] {appointmentId},
                        IDENTITY_TYPE);
        FetchResult<VideoSessionSummary> videoSession =
                downstreamClient.get(
                        videoRestClient,
                        "/api/v1/video/appointments/{appointmentId}/session",
                        new Object[] {appointmentId},
                        VIDEO_TYPE);
        FetchResult<SurveyAvailability> survey =
                downstreamClient.get(
                        surveyRestClient,
                        "/api/v1/survey/{appointmentId}",
                        new Object[] {appointmentId},
                        SURVEY_TYPE);

        ConsentState consent = mapConsent(platformConsent, clinicalConsent);
        IdentityState identityState = mapIdentity(identity);
        WaitingRoomState waitingRoom = deriveWaitingRoom(queue);
        VideoState video = mapVideo(videoSession);
        DocumentsState docs = deriveDocuments(documents);
        SurveyState surveyState = mapSurvey(survey);

        AggregationStatus overall =
                anyDegraded(
                                consent.status(),
                                identityState.status(),
                                waitingRoom.status(),
                                video.status(),
                                docs.status(),
                                surveyState.status())
                        ? AggregationStatus.DEGRADED
                        : AggregationStatus.OK;

        return new PatientJourneyResponse(
                appointmentId,
                overall,
                consent,
                identityState,
                waitingRoom,
                video,
                docs,
                surveyState);
    }

    private ConsentState mapConsent(
            FetchResult<Map<String, Object>> platformConsent,
            FetchResult<Map<String, Object>> clinicalConsent) {
        String platform = consentGranted(platformConsent) ? "GRANTED" : "PENDING";
        String clinical = consentGranted(clinicalConsent) ? "GRANTED" : "PENDING";
        AggregationStatus status =
                platformConsent.ok() && clinicalConsent.ok()
                        ? AggregationStatus.OK
                        : AggregationStatus.DEGRADED;
        return new ConsentState(status, platform, clinical);
    }

    private static boolean consentGranted(FetchResult<Map<String, Object>> consent) {
        if (!consent.ok() || consent.data() == null) {
            return false;
        }
        return Boolean.TRUE.equals(consent.data().get("granted"));
    }

    private IdentityState mapIdentity(FetchResult<IdentityStatus> identity) {
        if (identity.ok() && identity.data() != null) {
            return new IdentityState(AggregationStatus.OK, identity.data().verificationStatus());
        }
        return new IdentityState(AggregationStatus.DEGRADED, "PENDING");
    }

    private WaitingRoomState deriveWaitingRoom(FetchResult<QueuePosition> queue) {
        if (queue.ok() && queue.data() != null) {
            QueuePosition data = queue.data();
            return new WaitingRoomState(
                    AggregationStatus.OK,
                    data.position(),
                    data.estimatedWaitSec(),
                    "WAITING");
        }
        return new WaitingRoomState(AggregationStatus.DEGRADED, 0, 0, "UNKNOWN");
    }

    private VideoState mapVideo(FetchResult<VideoSessionSummary> videoSession) {
        if (videoSession.ok() && videoSession.data() != null) {
            VideoSessionSummary session = videoSession.data();
            boolean available =
                    session.status() != null && !session.status().equalsIgnoreCase("ENDED");
            return new VideoState(
                    AggregationStatus.OK, available, available ? session.sessionId() : null);
        }
        return new VideoState(AggregationStatus.DEGRADED, false, null);
    }

    private DocumentsState deriveDocuments(FetchResult<ClinicalDocsSummary> documents) {
        if (documents.ok() && documents.data() != null) {
            return new DocumentsState(AggregationStatus.OK, documents.data().downloadUrl());
        }
        return new DocumentsState(AggregationStatus.DEGRADED, null);
    }

    private SurveyState mapSurvey(FetchResult<SurveyAvailability> survey) {
        if (survey.ok() && survey.data() != null) {
            return new SurveyState(AggregationStatus.OK, survey.data().available());
        }
        return new SurveyState(AggregationStatus.DEGRADED, false);
    }

    private static boolean anyDegraded(AggregationStatus... statuses) {
        for (AggregationStatus status : statuses) {
            if (status == AggregationStatus.DEGRADED) {
                return true;
            }
        }
        return false;
    }
}
