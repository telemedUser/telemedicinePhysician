package com.telemedicine.common.events.consumer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips processing when {@link ProcessedEventStore} shows this {@code eventId} was already handled.
 *
 * <p>Keyed on the envelope's {@code eventId}, not on the Kafka offset, so a redelivery after a
 * crash does not double-apply side effects.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotentConsumer {

    /** Consumer group used as the deduplication scope. Defaults to the service name. */
    String group() default "${spring.application.name:telemedicine}";
}
