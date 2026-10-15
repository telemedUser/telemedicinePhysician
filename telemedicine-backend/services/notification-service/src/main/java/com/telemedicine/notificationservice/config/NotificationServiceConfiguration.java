package com.telemedicine.notificationservice.config;

import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.notificationservice.adapter.NotifyChannelAdapter;
import com.telemedicine.notificationservice.client.IntegrationAppointmentClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationServiceConfiguration {

    @Bean
    RestClient integrationServiceRestClient(
            IntegrationHttpClientFactory clientFactory, NotificationServiceProperties properties) {
        return clientFactory.restClient(properties.getIntegrationServiceBaseUrl());
    }

    @Bean
    IntegrationAppointmentClient integrationAppointmentClient(RestClient integrationServiceRestClient) {
        return new IntegrationAppointmentClient(integrationServiceRestClient);
    }

    @Bean
    NotifyChannelAdapter notifyChannelAdapter(
            IntegrationHttpClientFactory clientFactory,
            com.telemedicine.common.integration.TelemedicineIntegrationProperties integrationProperties) {
        RestClient notifyClient = clientFactory.restClient(integrationProperties.getSimulator().getBaseUrl());
        return new NotifyChannelAdapter(notifyClient);
    }
}
