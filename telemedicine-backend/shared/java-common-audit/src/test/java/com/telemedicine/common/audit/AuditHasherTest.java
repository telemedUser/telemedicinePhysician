package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditHasherTest {

    private static final Instant AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void producesDeterministicHashForSameInput() {
        String hash1 =
                AuditHasher.hash(
                        "actor-1", "UPDATE", "config", "video.provider", "SUCCESS", AT, null);
        String hash2 =
                AuditHasher.hash(
                        "actor-1", "UPDATE", "config", "video.provider", "SUCCESS", AT, null);
        assertThat(hash1).isEqualTo(hash2).hasSize(64);
    }

    @Test
    void changesWhenPreviousHashChanges() {
        String first = AuditHasher.hash("actor-1", "A", "resource", "1", "SUCCESS", AT, "");
        String second = AuditHasher.hash("actor-2", "B", "resource", "2", "SUCCESS", AT, first);
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void treatsNullPreviousHashAsEmptyString() {
        String withNull = AuditHasher.hash("actor-1", "A", "resource", "1", "SUCCESS", AT, null);
        String withEmpty = AuditHasher.hash("actor-1", "A", "resource", "1", "SUCCESS", AT, "");
        assertThat(withNull).isEqualTo(withEmpty);
    }

    @Test
    void contentHashHandlesNonMapValues() {
        assertThat(AuditHasher.contentHash(new Payload("value-1"))).hasSize(64);
    }

    @Test
    void contentHashHandlesNull() {
        assertThat(AuditHasher.contentHash(null)).hasSize(64);
    }

    private record Payload(String value) {}

    @Test
    void verifyChainAcceptsIntactSequence() {
        AuditChainState chain = new AuditChainState();
        List<AuditEvent> events = new ArrayList<>();
        events.add(link(chain, "actor-1", "CREATE", "appointment", "appt-1", "SUCCESS", AT));
        events.add(
                link(
                        chain,
                        "actor-1",
                        "UPDATE",
                        "appointment",
                        "appt-1",
                        "SUCCESS",
                        AT.plusSeconds(1)));
        AuditHasher.verifyChain(events);
        assertThat(AuditHasher.isChainIntact(events)).isTrue();
    }

    @Test
    void verifyChainDetectsTamperedHash() {
        AuditChainState chain = new AuditChainState();
        AuditEvent first = link(chain, "actor-1", "CREATE", "appointment", "appt-1", "SUCCESS", AT);
        AuditEvent tampered =
                new AuditEvent(
                        first.auditId(),
                        first.actor(),
                        first.action(),
                        first.resourceType(),
                        first.resourceId(),
                        first.outcome(),
                        first.reason(),
                        first.beforeHash(),
                        first.afterHash(),
                        first.correlationId(),
                        first.occurredAt(),
                        first.sequence(),
                        first.previousHash(),
                        "deadbeef".repeat(8));
        assertThatThrownBy(() -> AuditHasher.verifyChain(List.of(tampered)))
                .isInstanceOf(AuditChainTamperException.class)
                .hasMessageContaining("Hash mismatch");
    }

    @Test
    void verifyChainDetectsBrokenLink() {
        AuditChainState chain = new AuditChainState();
        AuditEvent first = link(chain, "actor-1", "CREATE", "appointment", "appt-1", "SUCCESS", AT);
        AuditEvent second =
                link(
                        chain,
                        "actor-1",
                        "UPDATE",
                        "appointment",
                        "appt-1",
                        "SUCCESS",
                        AT.plusSeconds(1));
        AuditEvent broken =
                new AuditEvent(
                        second.auditId(),
                        second.actor(),
                        second.action(),
                        second.resourceType(),
                        second.resourceId(),
                        second.outcome(),
                        second.reason(),
                        second.beforeHash(),
                        second.afterHash(),
                        second.correlationId(),
                        second.occurredAt(),
                        second.sequence(),
                        "broken-previous-hash",
                        second.hash());
        assertThatThrownBy(() -> AuditHasher.verifyChain(List.of(first, broken)))
                .isInstanceOf(AuditChainTamperException.class)
                .hasMessageContaining("Previous hash mismatch");
    }

    private static AuditEvent link(
            AuditChainState chain,
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            String outcome,
            Instant occurredAt) {
        AuditChainState.ChainLink link = chain.nextLink();
        String hash =
                AuditHasher.hash(
                        actorId,
                        action,
                        resourceType,
                        resourceId,
                        outcome,
                        occurredAt,
                        link.previousHash());
        AuditEvent event =
                new AuditEvent(
                        "audit-" + link.sequence(),
                        new AuditActor(actorId, "PATIENT", null, null),
                        action,
                        resourceType,
                        resourceId,
                        AuditOutcome.valueOf(outcome),
                        null,
                        null,
                        null,
                        "corr-1",
                        occurredAt,
                        link.sequence(),
                        link.previousHash(),
                        hash);
        chain.advance(hash);
        return event;
    }
}
