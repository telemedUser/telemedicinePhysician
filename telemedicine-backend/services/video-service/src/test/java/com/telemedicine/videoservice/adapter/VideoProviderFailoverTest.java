package com.telemedicine.videoservice.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.telemedicine.common.integration.ProviderCategory;
import com.telemedicine.common.integration.ProviderRegistry;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.provider.ProviderSelector;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
class VideoProviderFailoverTest {

    private ProviderRegistry<VideoSessionAdapter> registry;
    private TelemedicineIntegrationProperties properties;
    private IntegrationHttpClientFactory clientFactory;

    @BeforeEach
    void setUp(WireMockRuntimeInfo runtime) {
        properties = new TelemedicineIntegrationProperties();
        properties.setConfigServiceBaseUrl(runtime.getHttpBaseUrl());
        properties.getSimulator().setBaseUrl(runtime.getHttpBaseUrl());
        clientFactory =
                new IntegrationHttpClientFactory(properties, List.of(), List.of(), null);
        registry = new ProviderRegistry<>();
        registry.register("vonage", new VonageVideoAdapter(clientFactory, properties));
        registry.register("acs", new AcsVideoAdapter(clientFactory, properties));
        registry.register("simulator", new SimulatorVideoAdapter(clientFactory, properties));
    }

    @Test
    void failsOverFromVonageToAcsWhenVonageIsUnhealthy() {
        stubConfig("vonage");
        WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/sim/vonage/session"))
                        .willReturn(WireMock.aResponse().withStatus(503)));
        WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/sim/acs/session"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"sessionId\":\"acs-failover\",\"provider\":\"acs\"}")));

        ProviderSelector<VideoSessionAdapter> selector = selector(name -> !"vonage".equals(name));

        VideoSessionAdapter active = selector.active();
        ProviderSession session = active.createSession("apt-failover");

        assertThat(active.providerName()).isEqualTo("acs");
        assertThat(session.sessionId()).isEqualTo("acs-failover");
    }

    @Test
    void usesConfiguredVonageWhenHealthy() {
        stubConfig("vonage");
        WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/sim/vonage/session"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"sessionId\":\"vonage-ok\",\"provider\":\"vonage\"}")));

        ProviderSelector<VideoSessionAdapter> selector = selector(name -> true);

        VideoSessionAdapter active = selector.active();
        assertThat(active.providerName()).isEqualTo("vonage");
        assertThat(active.createSession("apt-1").sessionId()).isEqualTo("vonage-ok");
    }

    private ProviderSelector<VideoSessionAdapter> selector(
            com.telemedicine.common.integration.provider.ProviderHealthChecker healthChecker) {
        return new ProviderSelector<>(
                registry,
                new PlatformConfigClient(
                        clientFactory.restClient(properties.getConfigServiceBaseUrl()),
                        new ObjectMapper(),
                        Duration.ofMinutes(1)),
                ProviderCategory.VIDEO,
                "simulator",
                healthChecker);
    }

    private static void stubConfig(String videoProvider) {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {"data":{"videoProvider":"%s","chatProvider":"simulator","telemetryEnabled":true}}
                                                """
                                                        .formatted(videoProvider))));
    }
}
