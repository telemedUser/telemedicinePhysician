package com.telemedicine.simulators.state;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class FonasaChallengeStore {

    private static final String SUCCESS_CODE = "123456";
    private static final int MAX_ATTEMPTS = 3;

    private final ConcurrentHashMap<String, FonasaChallenge> challenges = new ConcurrentHashMap<>();

    public FonasaChallenge create(String challengeId, String patientId) {
        FonasaChallenge challenge =
                new FonasaChallenge(
                        challengeId,
                        patientId,
                        SUCCESS_CODE,
                        0,
                        Instant.now().plus(5, ChronoUnit.MINUTES),
                        false);
        challenges.put(challengeId, challenge);
        return challenge;
    }

    public VerifyResult verify(String challengeId, String code) {
        FonasaChallenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            return VerifyResult.notFound();
        }
        if (challenge.lockedOut()) {
            return VerifyResult.lockedOut();
        }
        if (Instant.now().isAfter(challenge.expiresAt())) {
            return VerifyResult.expired();
        }
        if (SUCCESS_CODE.equals(code)) {
            challenges.remove(challengeId);
            return VerifyResult.success(challenge.patientId());
        }
        FonasaChallenge updated = challenge.incrementAttempts();
        challenges.put(challengeId, updated);
        if (updated.lockedOut()) {
            return VerifyResult.lockedOut();
        }
        return VerifyResult.invalid(attemptsRemaining(updated));
    }

    public Map<String, FonasaChallenge> snapshot() {
        return Map.copyOf(challenges);
    }

    public void clear() {
        challenges.clear();
    }

    public record VerifyResult(
            boolean verified, String patientId, String reason, int attemptsRemaining) {

        static VerifyResult success(String patientId) {
            return new VerifyResult(true, patientId, null, 0);
        }

        static VerifyResult invalid(int attemptsRemaining) {
            return new VerifyResult(false, null, "INVALID_CODE", attemptsRemaining);
        }

        static VerifyResult expired() {
            return new VerifyResult(false, null, "EXPIRED", 0);
        }

        static VerifyResult lockedOut() {
            return new VerifyResult(false, null, "LOCKED_OUT", 0);
        }

        static VerifyResult notFound() {
            return new VerifyResult(false, null, "NOT_FOUND", 0);
        }
    }

    public int attemptsRemaining(FonasaChallenge challenge) {
        return Math.max(0, MAX_ATTEMPTS - challenge.attempts());
    }
}
