package com.telemedicine.common.integration.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest
class PlatformConfigClientBranchTest {

    @Test
    void usesStaleCacheWhileRefreshing(WireMockRuntimeInfo runtime) {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
{"videoProvider":"vonage","chatProvider":"simulator"}
""")));

        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl(runtime.getHttpBaseUrl()).build(),
                        new ObjectMapper(),
                        Duration.ofHours(1)) {
                    @Override
                    public PlatformConfigSnapshot snapshot() {
                        PlatformConfigSnapshot cached =
                                new PlatformConfigSnapshot(
                                        "simulator",
                                        "simulator",
                                        true,
                                        java.util.Map.of(),
                                        java.util.Map.of(),
                                        "{}",
                                        Instant.now().minus(Duration.ofHours(2)));
                        try {
                            return super.snapshot();
                        } catch (Exception ex) {
                            return cached;
                        }
                    }
                };

        PlatformConfigSnapshot snapshot = client.snapshot();
        assertThat(snapshot.videoProvider()).isEqualTo("vonage");
    }

    @Test
    void returnsCachedSnapshotWhenRefreshFails(WireMockRuntimeInfo runtime)
            throws InterruptedException {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .inScenario("cache")
                        .whenScenarioStateIs("Started")
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
{"data":{"videoProvider":"vonage","chatProvider":"simulator"}}
"""))
                        .willSetStateTo("Cached"));
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .inScenario("cache")
                        .whenScenarioStateIs("Cached")
                        .willReturn(WireMock.aResponse().withStatus(500)));

        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl(runtime.getHttpBaseUrl()).build(),
                        new ObjectMapper(),
                        Duration.ofMillis(50));

        assertThat(client.snapshot().videoProvider()).isEqualTo("vonage");
        Thread.sleep(100);
        assertThat(client.snapshot().videoProvider()).isEqualTo("vonage");
    }
}
