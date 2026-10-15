package com.telemedicine.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts the method to callers holding any one of the listed roles.
 *
 * <p>For the common single-role cases prefer {@link IsPatient}, {@link IsPhysician}, {@link
 * IsAdmin} or {@link IsService}; this exists for genuine either-or rules such as an endpoint a
 * physician and an administrator may both reach.
 *
 * <p>Enforced by {@link com.telemedicine.common.security.method.RequiresRoleAuthorizationManager}
 * rather than by a {@code @PreAuthorize} expression, so the role list is a compiled array rather
 * than a string that has to be assembled and parsed at runtime.
 *
 * <p>Values are bare realm role names ({@code PHYSICIAN}); the {@code ROLE_} prefix is added for
 * you, and passing an already-prefixed value works too.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequiresRole {

    /** Bare realm role names, for example {@code {"PHYSICIAN", "ADMIN"}}. */
    String[] value();
}
