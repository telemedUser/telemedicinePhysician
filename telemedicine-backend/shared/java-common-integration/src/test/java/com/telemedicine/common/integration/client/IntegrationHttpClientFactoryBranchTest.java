package com.telemedicine.common.integration.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationHttpClientFactoryBranchTest {

    @Test
    void appliesCustomizerWhenBuildingRestClient() {
        IntegrationHttpClientFactory factory =
                new IntegrationHttpClientFactory(properties(), List.of(), List.of(), null);

        assertThat(
                        factory.restClient(
                                "http://localhost",
                                builder -> builder.defaultHeader("X-Test", "yes")))
                .isNotNull();
    }

    private static TelemedicineIntegrationProperties properties() {
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.getHttp().setLogRequests(false);
        properties.getHttp().setConnectTimeout(java.time.Duration.ofDays(1));
        return properties;
    }
}
