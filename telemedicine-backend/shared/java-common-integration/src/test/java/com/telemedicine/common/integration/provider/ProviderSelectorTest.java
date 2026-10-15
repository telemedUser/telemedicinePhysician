package com.telemedicine.common.integration.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.telemedicine.common.integration.ProviderCategory;
import com.telemedicine.common.integration.ProviderRegistry;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest
class ProviderSelectorTest {

    private ProviderRegistry<String> registry;
    private List<String> changes;

    @BeforeEach
    void setUp() {
        registry = new ProviderRegistry<>();
        registry.register("simulator", "sim-client");
        registry.register("vonage", "vonage-client");
        registry.register("acs", "acs-client");
        changes = new ArrayList<>();
    }

    @Test
    void selectsConfiguredVideoProvider(WireMockRuntimeInfo runtime) {
        stubConfig(runtime, "vonage", "simulator");
        ProviderSelector<String> selector = selector(runtime, name -> true);
        selector.addListener((category, previous, current) -> changes.add(current));

        assertThat(selector.active()).isEqualTo("vonage-client");
        assertThat(changes).containsExactly("vonage");
    }

    @Test
    void failsOverWhenPreferredProviderIsUnhealthy(WireMockRuntimeInfo runtime) {
        stubConfig(runtime, "vonage", "simulator");
        ProviderSelector<String> selector = selector(runtime, name -> !"vonage".equals(name));

        assertThat(selector.active()).isEqualTo("sim-client");
    }

    @Test
    void usesSafeDefaultWhenConfiguredProviderMissing(WireMockRuntimeInfo runtime) {
        stubConfig(runtime, "unknown", "simulator");
        ProviderSelector<String> selector = selector(runtime, name -> true);

        assertThat(selector.configuredProviderName()).isEqualTo("simulator");
        assertThat(selector.active()).isEqualTo("sim-client");
    }

    @Test
    void usesCachedSnapshotWhenConfigServiceUnreachable() {
        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl("http://localhost:1").build(),
                        new ObjectMapper(),
                        Duration.ofMinutes(5)) {
                    @Override
                    public PlatformConfigSnapshot snapshot() {
                        return new PlatformConfigSnapshot(
                                "vonage",
                                "simulator",
                                true,
                                java.util.Map.of(),
                                java.util.Map.of(),
                                "{}",
                                Instant.now());
                    }
                };
        ProviderSelector<String> selector =
                new ProviderSelector<>(
                        registry, client, ProviderCategory.VIDEO, "simulator", name -> true);

        assertThat(selector.active()).isEqualTo("vonage-client");
    }

    private ProviderSelector<String> selector(
            WireMockRuntimeInfo runtime, ProviderHealthChecker healthChecker) {
        PlatformConfigClient client = configClient(runtime);
        return new ProviderSelector<>(
                registry, client, ProviderCategory.VIDEO, "simulator", healthChecker);
    }

    private PlatformConfigClient configClient(WireMockRuntimeInfo runtime) {
        RestClient restClient = RestClient.builder().baseUrl(runtime.getHttpBaseUrl()).build();
        return new PlatformConfigClient(restClient, new ObjectMapper(), Duration.ofMinutes(1));
    }

    private static void stubConfig(
            WireMockRuntimeInfo runtime, String videoProvider, String chatProvider) {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {
                                                  "data": {
                                                    "videoProvider": "%s",
                                                    "chatProvider": "%s",
                                                    "telemetryEnabled": true
                                                  }
                                                }
                                                """
                                                        .formatted(videoProvider, chatProvider))));
    }
}
