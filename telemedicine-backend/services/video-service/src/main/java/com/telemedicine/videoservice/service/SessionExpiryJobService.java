package com.telemedicine.videoservice.service;

import com.telemedicine.common.events.payload.VideoSessionEnded;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.videoservice.domain.SessionTokenEntity;
import com.telemedicine.videoservice.domain.VideoSessionEntity;
import com.telemedicine.videoservice.domain.VideoSessionStatus;
import com.telemedicine.videoservice.repository.SessionTokenRepository;
import com.telemedicine.videoservice.repository.VideoSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionExpiryJobService {

    private static final Logger log = LoggerFactory.getLogger(SessionExpiryJobService.class);

    private final VideoSessionRepository sessionRepository;
    private final SessionTokenRepository tokenRepository;
    private final OutboxRecorder outboxRecorder;

    public SessionExpiryJobService(
            VideoSessionRepository sessionRepository,
            SessionTokenRepository tokenRepository,
            OutboxRecorder outboxRecorder) {
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
        this.outboxRecorder = outboxRecorder;
    }

    @Transactional
    public int runCleanup() {
        Instant now = Instant.now();
        List<VideoSessionEntity> expired =
                sessionRepository.findByStatusNotAndExpiresAtBefore(VideoSessionStatus.ENDED, now);
        for (VideoSessionEntity session : expired) {
            endExpiredSession(session, now);
        }

        List<SessionTokenEntity> expiredTokens = tokenRepository.findByExpiresAtBefore(now);
        tokenRepository.deleteAll(expiredTokens);

        log.info(
                "Session expiry cleanup ended {} sessions and removed {} tokens",
                expired.size(),
                expiredTokens.size());
        return expired.size();
    }

    private void endExpiredSession(VideoSessionEntity session, Instant endedAt) {
        long durationSeconds =
                session.getStartedAt() == null
                        ? 0
                        : Duration.between(session.getStartedAt(), endedAt).getSeconds();
        session.setStatus(VideoSessionStatus.ENDED);
        session.setEndedAt(endedAt);
        session.setEndReason("EXPIRED");
        sessionRepository.save(session);

        outboxRecorder.record(
                "VideoSession",
                session.getId(),
                new VideoSessionEnded(
                        session.getAppointmentId(),
                        session.getId(),
                        session.getProvider(),
                        durationSeconds,
                        "EXPIRED",
                        endedAt),
                session.getAppointmentId(),
                session.getAppointmentId());
    }
}
