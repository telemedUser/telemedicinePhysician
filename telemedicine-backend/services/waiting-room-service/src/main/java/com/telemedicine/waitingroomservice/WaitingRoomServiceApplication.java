package com.telemedicine.waitingroomservice;

import com.telemedicine.common.persistence.EnableTelemedicinePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableTelemedicinePersistence
@EntityScan(basePackages = "com.telemedicine.waitingroomservice.domain")
@EnableJpaRepositories(basePackages = "com.telemedicine.waitingroomservice.repository")
public class WaitingRoomServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaitingRoomServiceApplication.class, args);
    }
}
