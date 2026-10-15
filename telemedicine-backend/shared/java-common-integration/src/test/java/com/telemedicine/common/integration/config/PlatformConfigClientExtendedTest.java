package com.telemedicine.common.integration.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@WireMockTest
class PlatformConfigClientExtendedTest {

    @Test
    void parsesNestedMapsAndInvalidatesCache(WireMockRuntimeInfo runtime) {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {
                                                  "data": {
                                                    "videoProvider": "acs",
                                                    "chatProvider": "simulator",
                                                    "notificationChannels": {"sms": false},
                                                    "mfeRemotes": {"admin": true},
                                                    "telemetryRules": "{\\"enabled\\":true}"
                                                  }
                                                }
                                                """)));

        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl(runtime.getHttpBaseUrl()).build(),
                        new ObjectMapper(),
                        Duration.ofMinutes(1));

        PlatformConfigSnapshot first = client.snapshot();
        client.invalidate();
        PlatformConfigSnapshot second = client.snapshot();

        assertThat(first.videoProvider()).isEqualTo("acs");
        assertThat(first.notificationChannels()).containsEntry("sms", false);
        assertThat(first.mfeRemotes()).containsEntry("admin", true);
        assertThat(first.telemetryRulesJson()).contains("enabled");
        assertThat(second.videoProvider()).isEqualTo("acs");
    }
}
