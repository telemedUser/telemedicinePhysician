package com.telemedicine.physicianservice.config;

import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PhysicianServiceConfiguration {

    /** Resolves the authenticated physician/service caller the same way across the service. */
    @Bean
    SecurityContextAccessor securityContextAccessor() {
        return new SecurityContextAccessor();
    }

    /** HTTP client to integration-service, the sole boundary to the HIS (TM-INT-001). */
    @Bean
    RestClient integrationServiceRestClient(
            IntegrationHttpClientFactory clientFactory, PhysicianServiceProperties properties) {
        return clientFactory.restClient(properties.getIntegrationServiceBaseUrl());
    }
}
