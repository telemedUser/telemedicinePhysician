package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.telemedicine.common.events.payload.ConsentGranted;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentAuditEventsTest {

    @Mock private OutboxRecorder outboxRecorder;

    @Mock private AuditEventDispatcher auditDispatcher;

    @Test
    void recordGrantedWritesConsentAndAuditEvents() {
        SecurityContextAccessor securityContext = new SecurityContextAccessor();
        TelemedicineAuditProperties properties = new TelemedicineAuditProperties();
        AuditChainState chainState = new AuditChainState();
        Instant grantedAt = Instant.parse("2026-01-01T00:00:00Z");

        ConsentAuditEvents.recordGranted(
                outboxRecorder,
                auditDispatcher,
                chainState,
                securityContext,
                properties,
                "consent-1",
                "patient-1",
                "PLATFORM_TERMS",
                "v2",
                "appt-1",
                grantedAt);

        ArgumentCaptor<ConsentGranted> consentCaptor =
                ArgumentCaptor.forClass(ConsentGranted.class);
        verify(outboxRecorder)
                .record(eq("consent"), eq("appt-1"), consentCaptor.capture(), any(), eq("appt-1"));
        assertThat(consentCaptor.getValue().consentId()).isEqualTo("consent-1");

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditDispatcher).dispatch(auditCaptor.capture());
        AuditEvent audit = auditCaptor.getValue();
        assertThat(audit.action()).isEqualTo(ConsentAuditEvents.CONSENT_GRANTED_ACTION);
        assertThat(audit.resourceType()).isEqualTo(ConsentAuditEvents.CONSENT_RESOURCE_TYPE);
        assertThat(audit.resourceId()).isEqualTo("consent-1");
        assertThat(audit.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(chainState.lastSequence()).isEqualTo(1L);
    }
}
