package com.telemedicine.identityservice.config;

import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.identityservice.client.IntegrationServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class IdentityServiceConfiguration {

    @Bean
    RestClient integrationServiceRestClient(
            IntegrationHttpClientFactory clientFactory, IdentityServiceProperties properties) {
        return clientFactory.restClient(properties.getIntegrationServiceBaseUrl());
    }

    @Bean
    IntegrationServiceClient integrationServiceClient(RestClient integrationServiceRestClient) {
        return new IntegrationServiceClient(integrationServiceRestClient);
    }
}
