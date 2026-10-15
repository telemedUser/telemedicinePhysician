package com.telemedicine.telemetryservice.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.telemedicine.common.core.error.IntegrationException;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
class AnuraAdapterTest {

    private AnuraAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo runtime) {
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.getSimulator().setBaseUrl(runtime.getHttpBaseUrl());
        IntegrationHttpClientFactory clientFactory =
                new IntegrationHttpClientFactory(properties, List.of(), List.of(), null);
        adapter =
                new AnuraAdapter(
                        new IntegrationFailureReporter(Optional.empty()), clientFactory, properties);

        WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/sim/anura/token"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"token\":\"anura-test-token\"}")));
        WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/sim/anura/decryptResults"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"heartRate\":72,\"stress\":0.31}")));
    }

    @Test
    void requestsTokenAndDecryptsResults() {
        AnuraToken token = adapter.requestToken("apt-telemetry");
        assertThat(token.token()).isEqualTo("anura-test-token");

        AnuraResults results = adapter.decryptResults("apt-telemetry", token.token());
        assertThat(results.metrics()).containsEntry("heartRate", 72);
    }

    @Test
    void propagatesSimulatorFailure() {
        WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/sim/anura/token"))
                        .willReturn(WireMock.aResponse().withStatus(503)));

        assertThatThrownBy(() -> adapter.requestToken("apt-down"))
                .isInstanceOf(IntegrationException.class);
    }
}
