package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.telemedicine.common.events.payload.ConsentWithdrawn;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentAuditEventsWithdrawnTest {

    @Mock private OutboxRecorder outboxRecorder;

    @Mock private AuditEventDispatcher auditDispatcher;

    @Test
    void recordWithdrawnWritesConsentAndAuditEvents() {
        Instant withdrawnAt = Instant.parse("2026-02-01T00:00:00Z");
        ConsentAuditEvents.recordWithdrawn(
                outboxRecorder,
                auditDispatcher,
                new AuditChainState(),
                new SecurityContextAccessor(),
                new TelemedicineAuditProperties(),
                "consent-9",
                "patient-9",
                "CLINICAL_INFORMED",
                "appt-9",
                withdrawnAt);

        ArgumentCaptor<ConsentWithdrawn> consentCaptor =
                ArgumentCaptor.forClass(ConsentWithdrawn.class);
        verify(outboxRecorder)
                .record(
                        org.mockito.ArgumentMatchers.eq("consent"),
                        org.mockito.ArgumentMatchers.eq("appt-9"),
                        consentCaptor.capture(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq("appt-9"));
        assertThat(consentCaptor.getValue().consentId()).isEqualTo("consent-9");

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditDispatcher).dispatch(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action())
                .isEqualTo(ConsentAuditEvents.CONSENT_WITHDRAWN_ACTION);
    }
}
