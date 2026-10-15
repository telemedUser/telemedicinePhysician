package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.telemedicine.common.events.payload.AuditRecorded;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxAuditDispatcherTest {

    @Mock private OutboxRecorder outboxRecorder;

    @Test
    void recordsAuditRecordedPayload() {
        OutboxAuditDispatcher dispatcher = new OutboxAuditDispatcher(outboxRecorder);
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        AuditEvent event =
                new AuditEvent(
                        "audit-1",
                        new AuditActor("user-1", "PATIENT", "127.0.0.1", "JUnit"),
                        "VIEW",
                        "DOCUMENT",
                        "doc-1",
                        AuditOutcome.SUCCESS,
                        null,
                        null,
                        null,
                        "corr-1",
                        occurredAt,
                        1L,
                        "",
                        AuditHasher.hash(
                                "user-1", "VIEW", "DOCUMENT", "doc-1", "SUCCESS", occurredAt, ""));

        dispatcher.dispatch(event);

        ArgumentCaptor<AuditRecorded> payloadCaptor = ArgumentCaptor.forClass(AuditRecorded.class);
        verify(outboxRecorder)
                .record(
                        eq("audit"),
                        eq("audit-1"),
                        payloadCaptor.capture(),
                        eq("corr-1"),
                        eq("audit-1"));
        AuditRecorded payload = payloadCaptor.getValue();
        assertThat(payload.auditId()).isEqualTo("audit-1");
        assertThat(payload.actorRef()).isEqualTo("user-1");
        assertThat(payload.action()).isEqualTo("VIEW");
        assertThat(payload.outcome()).isEqualTo("SUCCESS");
    }
}
