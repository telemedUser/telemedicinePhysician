package com.telemedicine.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Restricts the method to callers holding the patient role.
 *
 * <p>Meta-annotations are used in preference to raw {@code @PreAuthorize("hasRole('PATIENT')")}
 * because the expression is a string: a typo compiles, deploys, and fails open or closed with no
 * warning. There is exactly one copy of each expression, here.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasRole('PATIENT')")
public @interface IsPatient {}
