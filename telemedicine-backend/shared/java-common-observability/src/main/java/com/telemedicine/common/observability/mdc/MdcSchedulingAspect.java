package com.telemedicine.common.observability.mdc;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/** Restores MDC around {@code @Scheduled} methods so correlation survives background work. */
@Aspect
@SuppressWarnings("checkstyle:IllegalThrows")
public class MdcSchedulingAspect {

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object aroundScheduled(ProceedingJoinPoint joinPoint) throws Throwable {
        MdcContext context = MdcContext.capture();
        AutoCloseable scope = context.scope();
        try {
            return joinPoint.proceed();
        } finally {
            scope.close();
        }
    }
}
