package com.telemedicine.clinicaldocsservice;

import com.telemedicine.common.persistence.EnableTelemedicinePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableTelemedicinePersistence
@EntityScan(basePackages = "com.telemedicine.clinicaldocsservice.domain")
@EnableJpaRepositories(basePackages = "com.telemedicine.clinicaldocsservice.repository")
public class ClinicalDocsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicalDocsServiceApplication.class, args);
    }
}
