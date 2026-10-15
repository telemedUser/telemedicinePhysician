package com.telemedicine.common.core.phi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field, record component, parameter or accessor as carrying protected health information.
 *
 * <p>The annotation is load-bearing rather than documentary: {@link PhiSafeToString} masks
 * annotated members, the persistence module refuses to map an annotated column without an
 * encrypting converter, and an ArchUnit rule fails the build when an event payload declares one.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.FIELD,
    ElementType.RECORD_COMPONENT,
    ElementType.PARAMETER,
    ElementType.METHOD
})
public @interface Phi {

    /** Redaction strategy applied when the value is rendered into logs or diagnostics. */
    PhiKind value() default PhiKind.GENERIC;
}
