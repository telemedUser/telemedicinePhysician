package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.lang.reflect.Method;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditedAspectBranchCoverageTest {

    @Mock private AuditEventDispatcher dispatcher;

    @Mock private ProceedingJoinPoint joinPoint;

    @Mock private MethodSignature methodSignature;

    private AuditedAspect aspect;

    @BeforeEach
    void setUp() throws Throwable {
        TelemedicineAuditProperties properties = new TelemedicineAuditProperties();
        properties.setPhiSafeByDefault(false);
        aspect =
                new AuditedAspect(
                        dispatcher,
                        new SecurityContextAccessor(),
                        new AuditChainState(),
                        properties,
                        Optional.empty());

        Method method = Fixture.class.getDeclaredMethod("nullResource", String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getName()).thenReturn("nullResource");
        when(joinPoint.getArgs()).thenReturn(new Object[] {"ignored"});
        when(joinPoint.getThis()).thenReturn(new Fixture());
        org.mockito.Mockito.doReturn("ok").when(joinPoint).proceed();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void fallsBackToMethodNameWhenSpelResolvesNull() throws Throwable {
        aspect.audit(
                joinPoint,
                Fixture.class
                        .getDeclaredMethod("nullResource", String.class)
                        .getAnnotation(Audited.class));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(dispatcher).dispatch(captor.capture());
        assertThat(captor.getValue().resourceId()).isEqualTo("nullResource");
        assertThat(captor.getValue().actor().ip()).isEqualTo("198.51.100.20");
    }

    static class Fixture {

        @Audited(
                action = "READ",
                resourceType = "PATIENT",
                resourceIdExpression = "null",
                phiSafe = false,
                includeArgs = true)
        String nullResource(String ignored) {
            return "ok";
        }
    }
}
