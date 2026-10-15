package com.telemedicine.simulators.state;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class VideoSessionStore {

    public record VideoSession(
            String sessionId, String provider, String appointmentId, Instant createdAt) {}

    private final ConcurrentHashMap<String, VideoSession> sessionsByProvider = new ConcurrentHashMap<>();

    public VideoSession create(String provider, String sessionId, String appointmentId) {
        VideoSession session = new VideoSession(sessionId, provider, appointmentId, Instant.now());
        sessionsByProvider.put(provider + ":" + sessionId, session);
        return session;
    }

    public VideoSession requireSession(String provider, String sessionId) {
        return sessionsByProvider.get(provider + ":" + sessionId);
    }

    public void remove(String provider, String sessionId) {
        sessionsByProvider.remove(provider + ":" + sessionId);
    }

    public Map<String, VideoSession> snapshot() {
        return Map.copyOf(sessionsByProvider);
    }

    public void clear() {
        sessionsByProvider.clear();
    }
}
