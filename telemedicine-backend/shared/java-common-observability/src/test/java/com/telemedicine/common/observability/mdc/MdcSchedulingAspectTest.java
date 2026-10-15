package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcSchedulingAspectTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void proceedsWithinCapturedMdc() throws Throwable {
        MDC.put("correlationId", "sched-corr");
        MdcSchedulingAspect aspect = new MdcSchedulingAspect();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("correlationId"));

        Object result = aspect.aroundScheduled(joinPoint);

        assertThat(result).isEqualTo("sched-corr");
    }
}
