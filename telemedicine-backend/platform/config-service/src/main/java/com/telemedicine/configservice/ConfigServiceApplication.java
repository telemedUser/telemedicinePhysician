package com.telemedicine.configservice;

import com.telemedicine.common.persistence.EnableTelemedicinePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableTelemedicinePersistence
@EnableCaching
@EntityScan(
        basePackages = {
            "com.telemedicine.common.persistence",
            "com.telemedicine.common.events.outbox",
            "com.telemedicine.configservice.domain"
        })
@EnableJpaRepositories(
        basePackages = {
            "com.telemedicine.common.persistence",
            "com.telemedicine.configservice.repository"
        })
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
