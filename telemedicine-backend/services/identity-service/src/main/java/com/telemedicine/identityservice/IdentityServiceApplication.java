package com.telemedicine.identityservice;

import com.telemedicine.common.persistence.EnableTelemedicinePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableTelemedicinePersistence
@ConfigurationPropertiesScan
@EntityScan(basePackages = "com.telemedicine.identityservice.domain")
@EnableJpaRepositories(basePackages = "com.telemedicine.identityservice.domain")
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
