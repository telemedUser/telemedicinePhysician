package com.telemedicine.common.security.consent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Refuses the invocation unless the subject has given the named consent.
 *
 * <p>The design document keeps platform terms and clinical informed consent separate and treats
 * proceeding without either as a compliance breach, not a validation error. Putting the check on
 * the annotation rather than in each method body means a new endpoint on a consented flow cannot
 * forget it.
 *
 * <p>Enforcement needs a {@link ConsentVerifier} bean. The default implementation grants
 * everything, so Phase 1 services behave exactly as they do today until {@code
 * waiting-room-service} supplies a real one in Phase 4.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequiresConsent {

    /** Consent type, for example {@code PLATFORM_TERMS} or {@code CLINICAL_INFORMED}. */
    String value();

    /** Minimum accepted version. Empty means any accepted version satisfies the gate. */
    String version() default "";

    /**
     * SpEL over the method arguments yielding the subject whose consent is checked, for example
     * {@code "#appointmentId"} or {@code "#request.patientId"}. Empty falls back to the patient
     * identifier of the authenticated caller.
     */
    String subject() default "";
}
