package com.telemedicine.common.integration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.constants.Headers;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

class SimScenarioPropagationBranchTest {

    @AfterEach
    void tearDown() {
        MDC.remove(Headers.SIM_SCENARIO);
    }

    @Test
    void skipsWhenScenarioAbsent() throws IOException {
        SimScenarioPropagationInterceptor interceptor = new SimScenarioPropagationInterceptor();
        ClientHttpRequest request = mock(ClientHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(Headers.SIM_SCENARIO)).isNull();
    }

    @Test
    void skipsWhenHeaderAlreadySet() throws IOException {
        SimScenarioPropagationInterceptor interceptor = new SimScenarioPropagationInterceptor();
        ClientHttpRequest request = mock(ClientHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.set(Headers.SIM_SCENARIO, "existing");
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        MDC.put(Headers.SIM_SCENARIO, "failure");
        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(Headers.SIM_SCENARIO)).isEqualTo("existing");
    }

    @Test
    void webClientSkipsWhenScenarioAbsent() {
        ExchangeFunction exchange = request -> Mono.empty();
        SimScenarioExchangeFilterFunction.create()
                .filter(
                        ClientRequest.create(
                                        org.springframework.http.HttpMethod.GET,
                                        java.net.URI.create("http://localhost"))
                                .build(),
                        exchange)
                .block();
    }

    @Test
    void webClientSkipsWhenHeaderAlreadyPresent() {
        ExchangeFunction exchange = request -> Mono.empty();
        MDC.put(Headers.SIM_SCENARIO, "failure");
        SimScenarioExchangeFilterFunction.create()
                .filter(
                        ClientRequest.create(
                                        org.springframework.http.HttpMethod.GET,
                                        java.net.URI.create("http://localhost"))
                                .header(Headers.SIM_SCENARIO, "existing")
                                .build(),
                        exchange)
                .block();
    }
}
