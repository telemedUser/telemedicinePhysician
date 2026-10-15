package com.telemedicine.simulators.state;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FonasaChallengeStoreTest {

    private FonasaChallengeStore store;

    @BeforeEach
    void setUp() {
        store = new FonasaChallengeStore();
    }

    @Test
    void verifySuccessWithCorrectCode() {
        var challenge = store.create("challenge-1", "patient-1");
        var result = store.verify(challenge.challengeId(), "123456");
        assertThat(result.verified()).isTrue();
        assertThat(result.patientId()).isEqualTo("patient-1");
    }

    @Test
    void lockoutAfterThreeFailures() {
        var challenge = store.create("challenge-2", "patient-2");
        store.verify(challenge.challengeId(), "000000");
        store.verify(challenge.challengeId(), "000000");
        var third = store.verify(challenge.challengeId(), "000000");
        assertThat(third.reason()).isEqualTo("LOCKED_OUT");
    }

    @Test
    void expiredChallengeWhenMissing() {
        var result = store.verify("missing", "123456");
        assertThat(result.reason()).isEqualTo("NOT_FOUND");
    }
}
