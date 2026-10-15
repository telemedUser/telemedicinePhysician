package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoggingAuditDispatcherTest {

    @Test
    void dispatchDoesNotThrow() {
        LoggingAuditDispatcher dispatcher = new LoggingAuditDispatcher();
        AuditEvent event =
                new AuditEvent(
                        "audit-1",
                        new AuditActor("user-1", "PATIENT", "127.0.0.1", "JUnit"),
                        "VIEW",
                        "DOCUMENT",
                        "12.345.678-5",
                        AuditOutcome.SUCCESS,
                        "patient@example.com viewed record",
                        null,
                        null,
                        "corr-1",
                        java.time.Instant.parse("2026-01-01T00:00:00Z"),
                        1L,
                        "",
                        "abc");
        dispatcher.dispatch(event);
        assertThat(event.auditId()).isEqualTo("audit-1");
    }
}
