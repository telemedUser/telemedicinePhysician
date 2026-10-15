package com.telemedicine.common.events;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Meta-annotation for domain-event listeners.
 *
 * <p>Wraps {@link KafkaListener} with the platform defaults: the topic name is the event type,
 * manual acknowledgement is expected, and the consumer group defaults to the service name.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@KafkaListener
public @interface DomainEventListener {

    /** Event type, which is also the topic name. */
    @AliasFor(annotation = KafkaListener.class, attribute = "topics")
    String[] value();

    @AliasFor(annotation = KafkaListener.class, attribute = "groupId")
    String groupId() default "${spring.application.name:telemedicine}";

    @AliasFor(annotation = KafkaListener.class, attribute = "containerFactory")
    String containerFactory() default "telemedicineKafkaListenerContainerFactory";
}
