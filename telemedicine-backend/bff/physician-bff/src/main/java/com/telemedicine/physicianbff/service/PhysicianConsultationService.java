package com.telemedicine.physicianbff.service;

import com.telemedicine.physicianbff.client.DownstreamClient;
import com.telemedicine.physicianbff.client.DownstreamClient.FetchResult;
import com.telemedicine.physicianbff.dto.AggregationStatus;
import com.telemedicine.physicianbff.dto.ChatContext;
import com.telemedicine.physicianbff.dto.ConsultationResponse;
import com.telemedicine.physicianbff.dto.PatientContext;
import com.telemedicine.physicianbff.dto.TelemetryContext;
import com.telemedicine.physicianbff.dto.VideoJoinPayload;
import com.telemedicine.physicianbff.dto.downstream.AppointmentSummary;
import com.telemedicine.physicianbff.dto.downstream.MpiPatientSummary;
import com.telemedicine.physicianbff.dto.downstream.TelemetryTokenResponse;
import com.telemedicine.physicianbff.dto.downstream.VideoSessionSummary;
import com.telemedicine.physicianbff.dto.downstream.VideoTokenResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PhysicianConsultationService {

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<AppointmentSummary>>
            APPOINTMENT_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<MpiPatientSummary>>
            MPI_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<VideoSessionSummary>>
            VIDEO_SESSION_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<VideoTokenResponse>>
            VIDEO_TOKEN_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<TelemetryTokenResponse>>
            TELEMETRY_TOKEN_TYPE = new ParameterizedTypeReference<>() {};

    private final DownstreamClient downstreamClient;
    private final RestClient integrationRestClient;
    private final RestClient videoRestClient;
    private final RestClient telemetryRestClient;

    public PhysicianConsultationService(
            DownstreamClient downstreamClient,
            @Qualifier("integrationRestClient") RestClient integrationRestClient,
            @Qualifier("videoRestClient") RestClient videoRestClient,
            @Qualifier("telemetryRestClient") RestClient telemetryRestClient) {
        this.downstreamClient = downstreamClient;
        this.integrationRestClient = integrationRestClient;
        this.videoRestClient = videoRestClient;
        this.telemetryRestClient = telemetryRestClient;
    }

    public ConsultationResponse consultation(String appointmentId) {
        FetchResult<AppointmentSummary> appointment =
                downstreamClient.get(
                        integrationRestClient,
                        "/v1/appointments/v2/{appointmentId}",
                        new Object[] {appointmentId},
                        APPOINTMENT_TYPE);
        FetchResult<MpiPatientSummary> mpi =
                downstreamClient.get(
                        integrationRestClient,
                        "/v1/appointments/v2/{appointmentId}/mpi",
                        new Object[] {appointmentId},
                        MPI_TYPE);

        FetchResult<VideoSessionSummary> videoSession =
                downstreamClient.get(
                        videoRestClient,
                        "/api/v1/video/appointments/{appointmentId}/session",
                        new Object[] {appointmentId},
                        VIDEO_SESSION_TYPE);

        VideoJoinPayload video = mapVideoJoin(videoSession);
        TelemetryContext telemetry = mapTelemetry(appointment, appointmentId, videoSession);
        ChatContext chat =
                new ChatContext(
                        videoSession.ok() ? AggregationStatus.OK : AggregationStatus.DEGRADED,
                        videoSession.ok() && videoSession.data() != null
                                ? "thread-" + videoSession.data().sessionId()
                                : null);
        PatientContext patientContext = derivePatientContext(appointment, mpi);

        AggregationStatus overall =
                anyDegraded(
                                video.status(),
                                chat.status(),
                                telemetry.status(),
                                patientContext.status())
                        ? AggregationStatus.DEGRADED
                        : AggregationStatus.OK;

        return new ConsultationResponse(
                appointmentId, overall, video, chat, telemetry, patientContext);
    }

    private VideoJoinPayload mapVideoJoin(FetchResult<VideoSessionSummary> videoSession) {
        if (!videoSession.ok() || videoSession.data() == null) {
            return new VideoJoinPayload(AggregationStatus.DEGRADED, null, null);
        }

        String sessionId = videoSession.data().sessionId();
        FetchResult<VideoTokenResponse> token =
                downstreamClient.post(
                        videoRestClient,
                        "/api/v1/video/sessions/{sessionId}/tokens",
                        new Object[] {sessionId},
                        Map.of("role", "PHYSICIAN"),
                        VIDEO_TOKEN_TYPE);

        if (token.ok() && token.data() != null) {
            return new VideoJoinPayload(
                    AggregationStatus.OK, sessionId, token.data().token());
        }
        return new VideoJoinPayload(AggregationStatus.DEGRADED, sessionId, null);
    }

    private TelemetryContext mapTelemetry(
            FetchResult<AppointmentSummary> appointment,
            String appointmentId,
            FetchResult<VideoSessionSummary> videoSession) {
        if (!videoSession.ok() || videoSession.data() == null) {
            return new TelemetryContext(AggregationStatus.DEGRADED, false);
        }

        Map<String, String> body = new HashMap<>();
        body.put("appointmentId", appointmentId);
        body.put("sessionId", videoSession.data().sessionId());
        if (appointment.ok() && appointment.data() != null) {
            body.put("appointmentType", appointment.data().source());
        }

        FetchResult<TelemetryTokenResponse> telemetryToken =
                downstreamClient.post(
                        telemetryRestClient,
                        "/api/v1/telemetry/token",
                        body,
                        TELEMETRY_TOKEN_TYPE);

        if (telemetryToken.ok() && telemetryToken.data() != null) {
            return new TelemetryContext(AggregationStatus.OK, true);
        }
        return new TelemetryContext(AggregationStatus.DEGRADED, false);
    }

    private PatientContext derivePatientContext(
            FetchResult<AppointmentSummary> appointment, FetchResult<MpiPatientSummary> mpi) {
        if (mpi.ok() && mpi.data() != null) {
            return new PatientContext(
                    AggregationStatus.OK,
                    mpi.data().patientId(),
                    mpi.data().name(),
                    appointment.ok() && appointment.data() != null
                            ? appointment.data().source()
                            : "sim-his");
        }
        if (appointment.ok() && appointment.data() != null) {
            return new PatientContext(
                    AggregationStatus.DEGRADED,
                    appointment.data().patientId(),
                    "Unknown",
                    appointment.data().source());
        }
        return new PatientContext(AggregationStatus.DEGRADED, null, null, "sim-his");
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
