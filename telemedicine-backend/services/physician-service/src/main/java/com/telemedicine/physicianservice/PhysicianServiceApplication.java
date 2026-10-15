package com.telemedicine.physicianservice;

import com.telemedicine.common.persistence.EnableTelemedicinePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableTelemedicinePersistence
@ConfigurationPropertiesScan
@EntityScan(basePackages = {"com.telemedicine.physicianservice.domain", "com.telemedicine.physicianservice.chat"})
@EnableJpaRepositories(basePackages = {"com.telemedicine.physicianservice.domain", "com.telemedicine.physicianservice.chat"})
public class PhysicianServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PhysicianServiceApplication.class, args);
    }
}
