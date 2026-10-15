package com.telemedicine.videoservice.config;

import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.common.integration.provider.ProviderHealthChecker;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;

@Configuration
@EnableConfigurationProperties(VideoServiceProperties.class)
public class VideoServiceConfiguration {

    @Bean
    ProviderHealthChecker videoProviderHealthChecker(
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties) {
        return providerKey -> {
            if ("simulator".equals(providerKey)) {
                return true;
            }
            if (!"vonage".equals(providerKey) && !"acs".equals(providerKey)) {
                return true;
            }
            try {
                clientFactory
                        .restClient(simulatorBaseUrl(properties) + "/sim/" + providerKey)
                        .post()
                        .uri("/session")
                        .body(Map.of("appointmentId", "__health__"))
                        .retrieve()
                        .toBodilessEntity();
                return true;
            } catch (RestClientException exception) {
                return false;
            }
        };
    }

    private static String simulatorBaseUrl(TelemedicineIntegrationProperties properties) {
        String baseUrl = properties.getSimulator().getBaseUrl();
        if (baseUrl.startsWith("lb://")) {
            return baseUrl.replace("lb://", "http://");
        }
        return baseUrl;
    }
}
