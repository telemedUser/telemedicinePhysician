package com.telemedicine.common.testing.jwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/** Installs a synthetic physician JWT in the test {@code SecurityContext}. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockPhysicianSecurityContextFactory.class)
public @interface WithMockPhysician {
    String username() default "physician.demo";
}
