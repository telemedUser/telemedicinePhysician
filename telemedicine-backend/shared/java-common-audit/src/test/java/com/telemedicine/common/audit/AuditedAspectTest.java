package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.error.ForbiddenException;
import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditedAspectTest {

    @Mock private AuditEventDispatcher dispatcher;

    @Mock private ProceedingJoinPoint joinPoint;

    @Mock private MethodSignature methodSignature;

    private AuditChainState chainState;
    private TelemedicineAuditProperties properties;
    private AuditedAspect aspect;

    @BeforeEach
    void setUp() throws Exception {
        chainState = new AuditChainState();
        properties = new TelemedicineAuditProperties();
        aspect =
                new AuditedAspect(
                        dispatcher,
                        new SecurityContextAccessor(),
                        chainState,
                        properties,
                        Optional.of(new SimpleMeterRegistry()));
        SecurityContextHolder.clearContext();

        Method method = AuditedFixtures.class.getDeclaredMethod("update", String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getName()).thenReturn("update");
        when(joinPoint.getArgs()).thenReturn(new Object[] {"appt-99"});
        when(joinPoint.getThis()).thenReturn(new AuditedFixtures());
    }

    @Test
    void recordsSuccessWithAuthenticatedActor() throws Throwable {
        authenticate("physician-1", Roles.PHYSICIAN);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result =
                aspect.audit(
                        joinPoint,
                        AuditedFixtures.class
                                .getMethod("update", String.class)
                                .getAnnotation(Audited.class));

        assertThat(result).isEqualTo("ok");
        AuditEvent event = captureEvent();
        assertThat(event.actorId()).isEqualTo("physician-1");
        assertThat(event.actorRole()).isEqualTo(Roles.REALM_PHYSICIAN);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.resourceId()).isEqualTo("appt-99");
        assertThat(event.sequence()).isEqualTo(1L);
        assertThat(AuditHasher.isChainIntact(List.of(event))).isTrue();
    }

    @Test
    void recordsFailureOutcomeOnException() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("failed"));

        assertThatThrownBy(
                        () ->
                                aspect.audit(
                                        joinPoint,
                                        AuditedFixtures.class
                                                .getMethod("fail", String.class)
                                                .getAnnotation(Audited.class)))
                .isInstanceOf(IllegalStateException.class);

        AuditEvent event = captureEvent();
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
    }

    @Test
    void recordsDeniedOutcomeForAccessDenied() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(
                        () ->
                                aspect.audit(
                                        joinPoint,
                                        AuditedFixtures.class
                                                .getMethod("deny", String.class)
                                                .getAnnotation(Audited.class)))
                .isInstanceOf(AccessDeniedException.class);

        AuditEvent event = captureEvent();
        assertThat(event.outcome()).isEqualTo(AuditOutcome.DENIED);
    }

    @Test
    void recordsDeniedOutcomeForForbiddenException() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new ForbiddenException("forbidden"));

        assertThatThrownBy(
                        () ->
                                aspect.audit(
                                        joinPoint,
                                        AuditedFixtures.class
                                                .getMethod("forbidden", String.class)
                                                .getAnnotation(Audited.class)))
                .isInstanceOf(ForbiddenException.class);

        AuditEvent event = captureEvent();
        assertThat(event.outcome()).isEqualTo(AuditOutcome.DENIED);
    }

    private AuditEvent captureEvent() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(dispatcher).dispatch(captor.capture());
        return captor.getValue();
    }

    private static void authenticate(String userId, String role) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                userId, "token", List.of(new SimpleGrantedAuthority(role))));
    }

    static class AuditedFixtures {

        @Audited(
                action = "APPOINTMENT_UPDATED",
                resourceType = "APPOINTMENT",
                resourceIdExpression = "#args[0]")
        public String update(String id) {
            return "ok";
        }

        @Audited(action = "APPOINTMENT_FAILED", resourceType = "APPOINTMENT")
        public String fail(String id) {
            throw new IllegalStateException("failed");
        }

        @Audited(action = "APPOINTMENT_DENIED", resourceType = "APPOINTMENT")
        public String deny(String id) {
            throw new AccessDeniedException("denied");
        }

        @Audited(action = "APPOINTMENT_FORBIDDEN", resourceType = "APPOINTMENT")
        public String forbidden(String id) {
            throw new ForbiddenException("forbidden");
        }
    }
}
