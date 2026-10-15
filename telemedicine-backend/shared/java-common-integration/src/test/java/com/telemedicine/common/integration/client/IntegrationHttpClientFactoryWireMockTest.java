package com.telemedicine.common.integration.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.client.RestClient;

@WireMockTest
class IntegrationHttpClientFactoryWireMockTest {

    @Test
    void forwardsSimScenarioHeader(WireMockRuntimeInfo runtime) {
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
                get(urlEqualTo("/his/patient"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withBody("{\"id\":\"1\"}")));

        IntegrationHttpClientFactory factory =
                new IntegrationHttpClientFactory(properties(), List.of(), List.of(), null);
        RestClient client = factory.restClient(runtime.getHttpBaseUrl());

        MDC.put(Headers.SIM_SCENARIO, "failure");
        try {
            String body = client.get().uri("/his/patient").retrieve().body(String.class);
            assertThat(body).contains("id");
        } finally {
            MDC.remove(Headers.SIM_SCENARIO);
        }

        com.github.tomakehurst.wiremock.client.WireMock.verify(
                getRequestedFor(urlEqualTo("/his/patient"))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("failure")));
    }

    private static TelemedicineIntegrationProperties properties() {
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.getHttp().setLogRequests(false);
        return properties;
    }
}
