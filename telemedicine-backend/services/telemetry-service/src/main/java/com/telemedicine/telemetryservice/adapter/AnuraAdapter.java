package com.telemedicine.telemetryservice.adapter;

import com.telemedicine.common.integration.ExternalSystems;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.AntiCorruptionMapper;
import com.telemedicine.common.integration.acl.ExternalSystemAdapter;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class AnuraAdapter
        extends ExternalSystemAdapter<
                AnuraTokenRequest, Map<String, String>, Map<String, String>, AnuraToken> {

    private static final ParameterizedTypeReference<Map<String, String>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<Map<String, Object>> OBJECT_MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final IntegrationHttpClientFactory clientFactory;
    private final TelemedicineIntegrationProperties properties;

    public AnuraAdapter(
            IntegrationFailureReporter failureReporter,
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties) {
        super(
                ExternalSystems.ANURA,
                failureReporter,
                new AntiCorruptionMapper<>() {
                    @Override
                    public Map<String, String> toExternal(AnuraTokenRequest domainRequest) {
                        return Map.of("appointmentId", domainRequest.appointmentId());
                    }

                    @Override
                    public AnuraToken toDomain(Map<String, String> externalResponse) {
                        return new AnuraToken(externalResponse.get("token"));
                    }
                });
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    public AnuraToken requestToken(String appointmentId) {
        return execute(new AnuraTokenRequest(appointmentId), "token", appointmentId);
    }

    public AnuraResults decryptResults(String appointmentId, String token) {
        Map<String, Object> response =
                client()
                        .post()
                        .uri("/decryptResults")
                        .body(Map.of("token", token))
                        .retrieve()
                        .body(OBJECT_MAP_TYPE);
        if (response == null) {
            response = Map.of();
        }
        return new AnuraResults(response);
    }

    @Override
    protected Map<String, String> invoke(Map<String, String> externalRequest) {
        Map<String, String> response =
                client().post().uri("/token").body(externalRequest).retrieve().body(MAP_TYPE);
        if (response == null) {
            throw new IllegalStateException("ANURA token response was empty");
        }
        return response;
    }

    private org.springframework.web.client.RestClient client() {
        return clientFactory.restClient(simulatorBaseUrl() + "/sim/anura");
    }

    private String simulatorBaseUrl() {
        String baseUrl = properties.getSimulator().getBaseUrl();
        if (baseUrl.startsWith("lb://")) {
            return baseUrl.replace("lb://", "http://");
        }
        return baseUrl;
    }
}
