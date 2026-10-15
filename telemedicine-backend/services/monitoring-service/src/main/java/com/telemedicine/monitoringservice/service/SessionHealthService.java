package com.telemedicine.monitoringservice.service;

import com.telemedicine.monitoringservice.domain.SessionHealthEntity;
import com.telemedicine.monitoringservice.domain.SessionHealthRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionHealthService {

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ENDED = "ENDED";

    private final SessionHealthRepository sessionHealthRepository;

    public SessionHealthService(SessionHealthRepository sessionHealthRepository) {
        this.sessionHealthRepository = sessionHealthRepository;
    }

    @Transactional
    public void onSessionCreated(String sessionId, String appointmentId, String provider, Instant createdAt) {
        SessionHealthEntity entity = sessionHealthRepository.findById(sessionId).orElseGet(SessionHealthEntity::new);
        entity.setSessionId(sessionId);
        entity.setAppointmentId(appointmentId);
        entity.setProvider(provider);
        entity.setStatus(STATUS_CREATED);
        entity.setUpdatedAt(Instant.now());
        sessionHealthRepository.save(entity);
    }

    @Transactional
    public void onSessionStarted(String sessionId, String appointmentId, String provider, Instant startedAt) {
        SessionHealthEntity entity = sessionHealthRepository.findById(sessionId).orElseGet(SessionHealthEntity::new);
        entity.setSessionId(sessionId);
        entity.setAppointmentId(appointmentId);
        entity.setProvider(provider);
        entity.setStatus(STATUS_ACTIVE);
        entity.setStartedAt(startedAt);
        entity.setUpdatedAt(Instant.now());
        sessionHealthRepository.save(entity);
    }

    @Transactional
    public void onSessionEnded(String sessionId, Instant endedAt) {
        sessionHealthRepository
                .findById(sessionId)
                .ifPresent(
                        entity -> {
                            entity.setStatus(STATUS_ENDED);
                            entity.setEndedAt(endedAt);
                            entity.setUpdatedAt(Instant.now());
                            sessionHealthRepository.save(entity);
                        });
    }

    @Transactional
    public void recordQuality(String sessionId, int qualityScore, Instant recordedAt) {
        sessionHealthRepository
                .findById(sessionId)
                .ifPresent(
                        entity -> {
                            entity.setQualityScore(qualityScore);
                            entity.setLastQualityAt(recordedAt);
                            entity.setUpdatedAt(Instant.now());
                            sessionHealthRepository.save(entity);
                        });
    }

    public long countActiveSessions() {
        return sessionHealthRepository.countByStatus(STATUS_ACTIVE);
    }

    public Double averageConnectionQuality() {
        var average =
                sessionHealthRepository.findByStatus(STATUS_ACTIVE).stream()
                        .filter(session -> session.getQualityScore() != null)
                        .mapToInt(SessionHealthEntity::getQualityScore)
                        .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

    public SessionHealthEntity requireSession(String sessionId) {
        return sessionHealthRepository
                .findById(sessionId)
                .orElseGet(
                        () -> {
                            SessionHealthEntity entity = new SessionHealthEntity();
                            entity.setSessionId(sessionId);
                            entity.setAppointmentId("unknown");
                            entity.setStatus(STATUS_CREATED);
                            entity.setUpdatedAt(Instant.now());
                            return entity;
                        });
    }
}
