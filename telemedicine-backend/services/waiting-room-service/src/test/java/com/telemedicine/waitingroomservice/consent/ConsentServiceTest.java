package com.telemedicine.waitingroomservice.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.telemedicine.common.audit.AuditChainState;
import com.telemedicine.common.audit.AuditEventDispatcher;
import com.telemedicine.common.audit.TelemedicineAuditProperties;
import com.telemedicine.common.core.error.ConsentRequiredException;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.consent.ConsentDecision;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.domain.ConsentEntity;
import com.telemedicine.waitingroomservice.repository.ConsentRepository;
import com.telemedicine.waitingroomservice.web.dto.ConsentGrantRequest;
import com.telemedicine.waitingroomservice.web.dto.ConsentWithdrawRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock private ConsentRepository consentRepository;
    @Mock private OutboxRecorder outboxRecorder;
    @Mock private AuditEventDispatcher auditDispatcher;

    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        WaitingRoomServiceProperties properties = new WaitingRoomServiceProperties();
        properties.setPlatformConsentVersion("1.0");
        properties.setClinicalConsentVersion("1.0");
        consentService =
                new ConsentService(
                        consentRepository,
                        outboxRecorder,
                        auditDispatcher,
                        new AuditChainState(),
                        new SecurityContextAccessor(),
                        new TelemedicineAuditProperties(),
                        properties);
    }

    @Test
    void grantPlatformPersistsConsentAndRecordsEvent() {
        ConsentGrantRequest request = new ConsentGrantRequest("appt-1", null);
        org.mockito.Mockito.when(consentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = consentService.grantPlatform(request);

        assertThat(response.status()).isEqualTo("GRANTED");
        verify(outboxRecorder).record(any(), any(), any(), any(), any());
        verify(auditDispatcher).dispatch(any());
    }

    @Test
    void verifyReturnsMissingWhenNoConsent() {
        org.mockito.Mockito.when(
                        consentRepository.findActiveBySubjectAndType("pat-1", "PLATFORM_TERMS"))
                .thenReturn(Optional.empty());

        ConsentDecision decision = consentService.verify("pat-1", "PLATFORM_TERMS", "1.0");

        assertThat(decision.granted()).isFalse();
        assertThat(decision.reason().name()).isEqualTo("CONSENT_REQUIRED");
    }

    @Test
    void withdrawBlocksFurtherProgression() {
        ConsentEntity active = new ConsentEntity();
        active.setId("c-1");
        active.setSubjectId("pat-1");
        active.setAppointmentId("appt-1");
        active.setConsentType("PLATFORM_TERMS");
        active.setVersion("1.0");
        active.setGrantedAt(Instant.now());
        org.mockito.Mockito.when(consentRepository.findByAppointmentIdAndWithdrawnAtIsNull("appt-1"))
                .thenReturn(List.of(active));
        org.mockito.Mockito.when(consentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        consentService.withdraw(new ConsentWithdrawRequest("appt-1"));

        assertThatThrownBy(() -> consentService.requireBothConsents("pat-1"))
                .isInstanceOf(ConsentRequiredException.class);
    }
}
