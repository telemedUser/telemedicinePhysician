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
class PlatformConfigClientTest {

    @Test
    void parsesConfigSnapshot(WireMockRuntimeInfo runtime) {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {
                                                  "data": {
                                                    "videoProvider": "vonage",
                                                    "chatProvider": "zendesk",
                                                    "telemetryEnabled": false,
                                                    "notificationChannels": {"email": true}
                                                  }
                                                }
                                                """)));

        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl(runtime.getHttpBaseUrl()).build(),
                        new ObjectMapper(),
                        Duration.ofMinutes(1));

        PlatformConfigSnapshot snapshot = client.snapshot();

        assertThat(snapshot.videoProvider()).isEqualTo("vonage");
        assertThat(snapshot.chatProvider()).isEqualTo("zendesk");
        assertThat(snapshot.telemetryEnabled()).isFalse();
        assertThat(snapshot.notificationChannels()).containsEntry("email", true);
    }

    @Test
    void returnsDefaultsWhenUnreachable() {
        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl("http://localhost:1").build(),
                        new ObjectMapper(),
                        Duration.ofMinutes(1));

        PlatformConfigSnapshot snapshot = client.snapshot();

        assertThat(snapshot.videoProvider()).isEqualTo("simulator");
        assertThat(snapshot.chatProvider()).isEqualTo("simulator");
    }
}
