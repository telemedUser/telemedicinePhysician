package com.telemedicine.videoservice.adapter;

import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClientException;

abstract class AbstractSimulatorVideoAdapter implements VideoSessionAdapter {

    private static final ParameterizedTypeReference<Map<String, String>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final IntegrationHttpClientFactory clientFactory;
    private final TelemedicineIntegrationProperties properties;
    private final String providerKey;
    private final String simPath;

    protected AbstractSimulatorVideoAdapter(
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties,
            String providerKey,
            String simPath) {
        this.clientFactory = clientFactory;
        this.properties = properties;
        this.providerKey = providerKey;
        this.simPath = simPath;
    }

    @Override
    public String providerName() {
        return providerKey;
    }

    @Override
    public ProviderSession createSession(String appointmentId) {
        Map<String, String> response =
                client()
                        .post()
                        .uri("/session")
                        .body(Map.of("appointmentId", appointmentId))
                        .retrieve()
                        .body(MAP_TYPE);
        if (response == null || response.get("sessionId") == null) {
            throw new RestClientException("Provider returned empty session response");
        }
        return new ProviderSession(response.get("sessionId"), providerKey);
    }

    @Override
    public ProviderToken issueToken(String providerSessionId, String role) {
        Map<String, String> response =
                client()
                        .post()
                        .uri("/token")
                        .body(Map.of("sessionId", providerSessionId, "role", role))
                        .retrieve()
                        .body(MAP_TYPE);
        if (response == null || response.get("token") == null) {
            throw new RestClientException("Provider returned empty token response");
        }
        return new ProviderToken(response.get("token"), role);
    }

    private org.springframework.web.client.RestClient client() {
        return clientFactory.restClient(simulatorBaseUrl() + simPath);
    }

    private String simulatorBaseUrl() {
        String baseUrl = properties.getSimulator().getBaseUrl();
        if (baseUrl.startsWith("lb://")) {
            return baseUrl.replace("lb://", "http://");
        }
        return baseUrl;
    }
}
