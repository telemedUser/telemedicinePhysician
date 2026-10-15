package com.telemedicine.integrationservice;

import com.telemedicine.common.persistence.EnableTelemedicinePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableTelemedicinePersistence
@EntityScan(
        basePackages = {
            "com.telemedicine.common.persistence",
            "com.telemedicine.common.events.outbox",
            "com.telemedicine.integrationservice.infrastructure.persistence"
        })
@EnableJpaRepositories(
        basePackages = {
            "com.telemedicine.common.persistence",
            "com.telemedicine.integrationservice.infrastructure.persistence"
        })
public class IntegrationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationServiceApplication.class, args);
    }
}
