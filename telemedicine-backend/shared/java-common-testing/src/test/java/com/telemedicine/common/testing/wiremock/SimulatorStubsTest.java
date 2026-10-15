package com.telemedicine.common.testing.wiremock;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.telemedicine.common.core.constants.Headers;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulatorStubsTest {

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(0);
        server.start();
        SimulatorStubs.registerAll(server);
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void successScenarioReturnsVendorPayload() throws Exception {
        HttpResponse<String> response = get("/sim/his/appointments/apt-001", "success");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("apt-001");
    }

    @Test
    void failureScenarioReturns503() throws Exception {
        HttpResponse<String> response = post("/sim/fonasa/2fa/verify", "failure");
        assertThat(response.statusCode()).isEqualTo(503);
    }

    @Test
    void timeoutScenarioReturns504() throws Exception {
        HttpResponse<String> response = post("/sim/vonage/session", "timeout");
        assertThat(response.statusCode()).isEqualTo(504);
    }

    private HttpResponse<String> get(String path, String scenario) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(server.baseUrl() + path))
                        .header(Headers.SIM_SCENARIO, scenario)
                        .GET()
                        .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String scenario) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(server.baseUrl() + path))
                        .header(Headers.SIM_SCENARIO, scenario)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
