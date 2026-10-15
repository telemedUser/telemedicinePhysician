package com.telemedicine.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Restricts the method to machine-to-machine callers.
 *
 * <p>Use on internal endpoints that another service calls with a Keycloak service-account token, so
 * they cannot be reached with an end-user token that happens to have a broad role.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasRole('SERVICE')")
public @interface IsService {}
