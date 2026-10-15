package com.telemedicine.common.integration.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@WireMockTest
class PlatformConfigClientErrorTest {

    @Test
    void wrapsParseErrors(WireMockRuntimeInfo runtime) {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("not-json")));

        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl(runtime.getHttpBaseUrl()).build(),
                        new ObjectMapper(),
                        Duration.ofMinutes(1));

        assertThatThrownBy(client::snapshot).isInstanceOf(RestClientException.class);
    }

    @Test
    void invalidateForcesRefetch(WireMockRuntimeInfo runtime) {
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/v1/config"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
{"data":{"videoProvider":"vonage","chatProvider":"simulator"}}
""")));

        PlatformConfigClient client =
                new PlatformConfigClient(
                        RestClient.builder().baseUrl(runtime.getHttpBaseUrl()).build(),
                        new ObjectMapper(),
                        Duration.ofHours(1));

        client.snapshot();
        client.invalidate();
        assertThatCode(client::snapshot).doesNotThrowAnyException();
    }
}
