package com.telemedicine.common.integration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * Marks a bean as a pluggable provider implementation, for example {@code vonage} or {@code acs}.
 *
 * <p>Beans are registered automatically into the matching {@link ProviderRegistry} by integration
 * auto-configuration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ProviderAdapter {

    /** Registered provider key, for example {@code vonage}. */
    String value();

    /** Which config-service field drives selection for this adapter. */
    ProviderCategory category() default ProviderCategory.VIDEO;
}
