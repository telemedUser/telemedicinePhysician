package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditChainVerifierTest {

    @Test
    void isChainIntactReturnsFalseForTamperedEvent() {
        AuditEvent event =
                new AuditEvent(
                        "audit-1",
                        new AuditActor("actor", "PATIENT", null, null),
                        "ACTION",
                        "TYPE",
                        "id",
                        AuditOutcome.SUCCESS,
                        null,
                        null,
                        null,
                        "corr",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        1L,
                        "",
                        "bad-hash");
        assertThat(AuditHasher.isChainIntact(java.util.List.of(event))).isFalse();
    }

    @Test
    void verifyChainDetectsSequenceGap() {
        AuditChainState chain = new AuditChainState();
        AuditEvent first =
                linkedEvent(chain, 1L, "", "actor", Instant.parse("2026-01-01T00:00:00Z"));
        AuditEvent gap =
                new AuditEvent(
                        "audit-2",
                        first.actor(),
                        first.action(),
                        first.resourceType(),
                        first.resourceId(),
                        first.outcome(),
                        null,
                        null,
                        null,
                        "corr",
                        first.occurredAt(),
                        3L,
                        first.hash(),
                        first.hash());
        assertThatThrownBy(() -> AuditHasher.verifyChain(java.util.List.of(first, gap)))
                .isInstanceOf(AuditChainTamperException.class)
                .hasMessageContaining("Sequence gap");
    }

    private static AuditEvent linkedEvent(
            AuditChainState chain, long sequence, String previousHash, String actor, Instant at) {
        String hash = AuditHasher.hash(actor, "ACTION", "TYPE", "id", "SUCCESS", at, previousHash);
        AuditEvent event =
                new AuditEvent(
                        "audit-" + sequence,
                        new AuditActor(actor, "PATIENT", null, null),
                        "ACTION",
                        "TYPE",
                        "id",
                        AuditOutcome.SUCCESS,
                        null,
                        null,
                        null,
                        "corr",
                        at,
                        sequence,
                        previousHash,
                        hash);
        chain.advance(hash);
        return event;
    }
}
