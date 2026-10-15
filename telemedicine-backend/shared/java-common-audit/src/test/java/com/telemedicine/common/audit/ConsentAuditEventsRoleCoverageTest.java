package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ConsentAuditEventsRoleCoverageTest {

    @Mock private com.telemedicine.common.persistence.outbox.OutboxRecorder outboxRecorder;

    @Mock private AuditEventDispatcher auditDispatcher;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesAdminRoleForAuthenticatedActor() {
        authenticate("admin-1", Roles.ADMIN);
        recordAndAssertRole(Roles.REALM_ADMIN);
    }

    @Test
    void resolvesServiceRoleForAuthenticatedActor() {
        authenticate("svc-1", Roles.SERVICE);
        recordAndAssertRole(Roles.REALM_SERVICE);
    }

    private void recordAndAssertRole(String expectedRole) {
        ConsentAuditEvents.recordGranted(
                outboxRecorder,
                auditDispatcher,
                new AuditChainState(),
                new SecurityContextAccessor(),
                new TelemedicineAuditProperties(),
                "consent-1",
                "subject-1",
                "PLATFORM_TERMS",
                "v1",
                "appt-1",
                Instant.parse("2026-01-01T00:00:00Z"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditDispatcher).dispatch(captor.capture());
        assertThat(captor.getValue().actorRole()).isEqualTo(expectedRole);
    }

    private static void authenticate(String userId, String role) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                userId, "token", List.of(new SimpleGrantedAuthority(role))));
    }
}
