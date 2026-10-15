package com.telemedicine.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    String action();

    String resourceType();

    /**
     * SpEL expression evaluated against method parameters to resolve the resource identifier, for
     * example {@code #appointmentId}. When blank the method name is used.
     */
    String resourceIdExpression() default "";

    /** Outcome recorded when the advised method throws. */
    AuditOutcome outcomeOnException() default AuditOutcome.FAILURE;

    /** When true, argument and return-value digests are captured as before/after hashes. */
    boolean includeArgs() default false;

    /** When true, free-text reason fields and argument digests are PHI-redacted before dispatch. */
    boolean phiSafe() default true;
}
