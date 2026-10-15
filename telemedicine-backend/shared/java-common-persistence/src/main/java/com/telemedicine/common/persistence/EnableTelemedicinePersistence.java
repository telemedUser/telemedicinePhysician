package com.telemedicine.common.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Enables JPA entity scanning for shared persistence and outbox tables.
 *
 * <p>Without this, {@code OutboxEvent} is never mapped even though the repository is on the
 * classpath.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableJpaAuditing
@EntityScan(
        basePackages = {
            "com.telemedicine.common.persistence",
            "com.telemedicine.common.events.outbox"
        })
@EnableJpaRepositories(basePackages = "com.telemedicine.common.persistence")
public @interface EnableTelemedicinePersistence {}
