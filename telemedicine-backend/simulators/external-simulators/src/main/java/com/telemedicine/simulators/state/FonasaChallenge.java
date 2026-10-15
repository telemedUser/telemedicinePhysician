package com.telemedicine.simulators.state;

import java.time.Instant;

public record FonasaChallenge(
        String challengeId,
        String patientId,
        String code,
        int attempts,
        Instant expiresAt,
        boolean lockedOut) {

    FonasaChallenge incrementAttempts() {
        int nextAttempts = attempts + 1;
        boolean lock = nextAttempts >= 3;
        return new FonasaChallenge(challengeId, patientId, code, nextAttempts, expiresAt, lock);
    }
}
