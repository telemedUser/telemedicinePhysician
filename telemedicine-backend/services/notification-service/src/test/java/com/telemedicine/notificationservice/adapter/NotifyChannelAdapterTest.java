package com.telemedicine.notificationservice.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.telemedicine.common.testing.wiremock.SimulatorStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NotifyChannelAdapterTest {

    private WireMockServer wireMockServer;
    private NotifyChannelAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        SimulatorStubs.notify(wireMockServer);
        adapter = new NotifyChannelAdapter(RestClient.builder().baseUrl(wireMockServer.baseUrl()).build());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void sendsThroughSimNotifyEndpoint() {
        var result = adapter.send("sms", "pat-1", "Hello", null);

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isNotBlank();
    }

    @Test
    void propagatesChannelFailure() {
        wireMockServer.stubFor(
                post(urlPathEqualTo("/sim/notify/email"))
                        .willReturn(aResponse().withStatus(503).withBody("{\"message\":\"down\"}")));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> adapter.send("email", "pat-1", "Hello", "Subject"))
                .isInstanceOf(com.telemedicine.common.core.error.ProviderOutageException.class);
    }
}
