package com.telemedicine.telemetryservice;

import com.telemedicine.common.persistence.EnableTelemedicinePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableTelemedicinePersistence
@EntityScan(basePackages = "com.telemedicine.telemetryservice.domain")
@EnableJpaRepositories(basePackages = "com.telemedicine.telemetryservice.repository")
public class TelemetryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryServiceApplication.class, args);
    }
}
