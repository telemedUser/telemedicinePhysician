package com.telemedicine.videoservice.service;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IntegrationException;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.VideoSessionCreated;
import com.telemedicine.common.events.payload.VideoSessionEnded;
import com.telemedicine.common.events.payload.VideoSessionStarted;
import com.telemedicine.common.integration.provider.ProviderSelector;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.videoservice.adapter.ProviderSession;
import com.telemedicine.videoservice.adapter.ProviderToken;
import com.telemedicine.videoservice.adapter.VideoSessionAdapter;
import com.telemedicine.videoservice.config.VideoServiceProperties;
import com.telemedicine.videoservice.domain.SessionTokenEntity;
import com.telemedicine.videoservice.domain.VideoSessionEntity;
import com.telemedicine.videoservice.domain.VideoSessionStatus;
import com.telemedicine.videoservice.repository.SessionTokenRepository;
import com.telemedicine.videoservice.repository.VideoSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class VideoSessionService {

    private final VideoSessionRepository sessionRepository;
    private final SessionTokenRepository tokenRepository;
    private final ProviderSelector<Object> videoProviderSelector;
    private final OutboxRecorder outboxRecorder;
    private final VideoServiceProperties properties;
    private final VideoChatService chatService;
    private final JourneyMetrics journeyMetrics;

    public VideoSessionService(
            VideoSessionRepository sessionRepository,
            SessionTokenRepository tokenRepository,
            @Qualifier("videoProviderSelector") ProviderSelector<Object> videoProviderSelector,
            OutboxRecorder outboxRecorder,
            VideoServiceProperties properties,
            VideoChatService chatService,
            JourneyMetrics journeyMetrics) {
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
        this.videoProviderSelector = videoProviderSelector;
        this.outboxRecorder = outboxRecorder;
        this.properties = properties;
        this.chatService = chatService;
        this.journeyMetrics = journeyMetrics;
    }

    @Transactional
    public Map<String, String> createSession(String appointmentId, String recordingPolicy) {
        VideoSessionAdapter provider = activeVideoProvider();
        ProviderSession providerSession = provider.createSession(appointmentId);

        VideoSessionEntity session = new VideoSessionEntity();
        session.setId(IdGenerator.newUlid());
        session.setAppointmentId(appointmentId);
        session.setProvider(provider.providerName());
        session.setProviderSessionId(providerSession.sessionId());
        session.setRecordingPolicy(recordingPolicy);
        session.setStatus(VideoSessionStatus.CREATED);
        session.setExpiresAt(Instant.now().plus(properties.getSessionTtl()));
        sessionRepository.save(session);

        chatService.ensureThread(session);

        outboxRecorder.record(
                "VideoSession",
                session.getId(),
                new VideoSessionCreated(
                        appointmentId,
                        session.getId(),
                        session.getProvider(),
                        Instant.now()),
                correlationId(),
                appointmentId);

        return Map.of(
                "sessionId", session.getId(),
                "appointmentId", appointmentId,
                "provider", session.getProvider(),
                "providerSessionId", session.getProviderSessionId());
    }

    @Transactional
    public Map<String, String> issueToken(String sessionId, String role) {
        VideoSessionEntity session =
                sessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () -> new NotFoundException("Video session not found: " + sessionId));

        if (session.getStatus() == VideoSessionStatus.ENDED) {
            throw new IntegrationException(
                    ErrorCode.VIDEO_TOKEN_ISSUE_FAILED, session.getProvider(), "Session has ended");
        }

        VideoSessionAdapter provider = activeVideoProvider();
        try {
            ProviderToken providerToken =
                    provider.issueToken(session.getProviderSessionId(), role);

            SessionTokenEntity token = new SessionTokenEntity();
            token.setId(IdGenerator.newUlid());
            token.setSessionId(sessionId);
            token.setRole(role);
            token.setProviderToken(providerToken.token());
            token.setExpiresAt(Instant.now().plus(properties.getTokenTtl()));
            tokenRepository.save(token);

            return Map.of(
                    "tokenId", token.getId(),
                    "sessionId", sessionId,
                    "provider", provider.providerName(),
                    "token", providerToken.token(),
                    "role", role);
        } catch (RestClientException exception) {
            throw new IntegrationException(
                    ErrorCode.VIDEO_TOKEN_ISSUE_FAILED,
                    provider.providerName(),
                    "Failed to issue token",
                    exception);
        }
    }

    @Transactional
    public Map<String, Object> joinSession(String sessionId, String role) {
        VideoSessionEntity session = requireActiveSession(sessionId);
        if (session.getStatus() == VideoSessionStatus.CREATED) {
            session.setStatus(VideoSessionStatus.STARTED);
            session.setStartedAt(Instant.now());
            sessionRepository.save(session);

            outboxRecorder.record(
                    "VideoSession",
                    sessionId,
                    new VideoSessionStarted(
                            session.getAppointmentId(),
                            sessionId,
                            session.getProvider(),
                            session.getStartedAt()),
                    correlationId(),
                    session.getAppointmentId());

            journeyMetrics.recordVideoEnterTime(
                    journeyMetrics.startVideoEnterSample(), session.getAppointmentId());
        }
        return Map.of(
                "sessionId", sessionId,
                "status", session.getStatus().name(),
                "role", role);
    }

    @Transactional
    public Map<String, Object> leaveSession(String sessionId, String endReason) {
        VideoSessionEntity session = requireActiveSession(sessionId);
        if (session.getStatus() != VideoSessionStatus.ENDED) {
            Instant endedAt = Instant.now();
            long durationSeconds =
                    session.getStartedAt() == null
                            ? 0
                            : Duration.between(session.getStartedAt(), endedAt).getSeconds();
            session.setStatus(VideoSessionStatus.ENDED);
            session.setEndedAt(endedAt);
            session.setEndReason(endReason != null ? endReason : "COMPLETED");
            sessionRepository.save(session);

            outboxRecorder.record(
                    "VideoSession",
                    sessionId,
                    new VideoSessionEnded(
                            session.getAppointmentId(),
                            sessionId,
                            session.getProvider(),
                            durationSeconds,
                            session.getEndReason(),
                            endedAt),
                    correlationId(),
                    session.getAppointmentId());
        }
        return Map.of("sessionId", sessionId, "status", session.getStatus().name());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSession(String sessionId) {
        VideoSessionEntity session =
                sessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () -> new NotFoundException("Video session not found: " + sessionId));
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("appointmentId", session.getAppointmentId());
        response.put("provider", session.getProvider());
        response.put("status", session.getStatus().name());
        response.put("recordingPolicy", session.getRecordingPolicy());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSessionByAppointment(String appointmentId) {
        VideoSessionEntity session =
                sessionRepository
                        .findByAppointmentId(appointmentId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Video session not found for appointment: "
                                                        + appointmentId));
        return getSession(session.getId());
    }

    @Transactional
    public void recordQuality(String sessionId, Map<String, Object> sample) {
        requireActiveSession(sessionId);
        // Quality samples are persisted for monitoring pull; no catalogue event.
    }

    private VideoSessionEntity requireActiveSession(String sessionId) {
        return sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Video session not found: " + sessionId));
    }

    private VideoSessionAdapter activeVideoProvider() {
        try {
            return (VideoSessionAdapter) videoProviderSelector.active();
        } catch (RuntimeException exception) {
            throw new IntegrationException(
                    ErrorCode.VIDEO_PROVIDER_UNAVAILABLE,
                    "video",
                    "No healthy video provider available",
                    exception);
        }
    }

    private static String correlationId() {
        String fromMdc = MDC.get("correlationId");
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }

}
