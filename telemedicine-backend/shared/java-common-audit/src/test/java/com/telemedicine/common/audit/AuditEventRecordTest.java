package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEventRecordTest {

    @Test
    void exposesLegacyAndOutcomeAccessors() {
        AuditEvent event =
                new AuditEvent(
                        "audit-1",
                        new AuditActor("actor-1", "PHYSICIAN", "127.0.0.1", "agent"),
                        "VIEW",
                        "DOCUMENT",
                        "doc-1",
                        AuditOutcome.DENIED,
                        "reason",
                        "before",
                        "after",
                        "corr",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        2L,
                        "prev",
                        "hash");
        assertThat(event.actorId()).isEqualTo("actor-1");
        assertThat(event.actorRole()).isEqualTo("PHYSICIAN");
        assertThat(event.outcomeName()).isEqualTo("DENIED");
        assertThat(AuditActor.system("SYSTEM").id()).isEqualTo("system");
    }
}
