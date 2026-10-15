package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditedAspectExtendedTest {

    @Mock private AuditEventDispatcher dispatcher;

    @Mock private ProceedingJoinPoint joinPoint;

    @Mock private MethodSignature methodSignature;

    private SimpleMeterRegistry meterRegistry;
    private AuditedAspect aspect;

    @BeforeEach
    void setUp() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
        aspect =
                new AuditedAspect(
                        dispatcher,
                        new SecurityContextAccessor(),
                        new AuditChainState(),
                        new TelemedicineAuditProperties(),
                        Optional.of(meterRegistry));

        Method method = Fixture.class.getDeclaredMethod("withArgs", String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getName()).thenReturn("withArgs");
        when(joinPoint.getArgs()).thenReturn(new Object[] {"12.345.678-5"});
        when(joinPoint.getThis()).thenReturn(new Fixture());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.addHeader("User-Agent", "JUnit-Agent");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void capturesClientIpUserAgentAndArgumentHashes() throws Throwable {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "patient-1",
                                "token",
                                List.of(new SimpleGrantedAuthority(Roles.PATIENT))));
        when(joinPoint.proceed()).thenReturn("done");

        aspect.audit(
                joinPoint,
                Fixture.class
                        .getDeclaredMethod("withArgs", String.class)
                        .getAnnotation(Audited.class));

        org.mockito.ArgumentCaptor<AuditEvent> captor =
                org.mockito.ArgumentCaptor.forClass(AuditEvent.class);
        verify(dispatcher).dispatch(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.actor().ip()).isEqualTo("203.0.113.10");
        assertThat(event.actor().userAgent()).isEqualTo("JUnit-Agent");
        assertThat(event.beforeHash()).isNotBlank();
        assertThat(event.afterHash()).isNotBlank();
        assertThat(event.resourceId()).doesNotContain("12.345");
    }

    @Test
    void usesSystemActorWhenUnauthenticated() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withoutExpression", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getName()).thenReturn("withoutExpression");
        when(joinPoint.proceed()).thenReturn("done");

        aspect.audit(joinPoint, method.getAnnotation(Audited.class));

        org.mockito.ArgumentCaptor<AuditEvent> captor =
                org.mockito.ArgumentCaptor.forClass(AuditEvent.class);
        verify(dispatcher).dispatch(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.actorId()).isEqualTo("system");
        assertThat(event.resourceId()).isEqualTo("withoutExpression");
    }

    @Test
    void recordsDispatchFailuresWithoutBreakingCallerFlow() throws Throwable {
        Method method = Fixture.class.getDeclaredMethod("withoutExpression", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getName()).thenReturn("withoutExpression");
        when(joinPoint.proceed()).thenReturn("done");
        doThrow(new IllegalStateException("outbox down"))
                .when(dispatcher)
                .dispatch(org.mockito.ArgumentMatchers.any());

        Object result = aspect.audit(joinPoint, method.getAnnotation(Audited.class));

        assertThat(result).isEqualTo("done");
        assertThat(meterRegistry.get("telemedicine.audit.dispatch.failures").counter().count())
                .isEqualTo(1.0);
    }

    static class Fixture {

        @Audited(
                action = "UPDATE",
                resourceType = "PATIENT",
                resourceIdExpression = "#args[0]",
                includeArgs = true)
        String withArgs(String rut) {
            return rut;
        }

        @Audited(action = "READ", resourceType = "PATIENT")
        String withoutExpression(String ignored) {
            return "ok";
        }
    }
}
