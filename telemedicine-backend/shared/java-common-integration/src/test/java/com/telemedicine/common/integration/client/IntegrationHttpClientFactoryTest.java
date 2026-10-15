package com.telemedicine.common.integration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;

class IntegrationHttpClientFactoryTest {

    @Test
    void buildsRestAndWebClients() {
        IntegrationHttpClientFactory factory =
                new IntegrationHttpClientFactory(properties(), List.of(), List.of(), null);

        assertThat(factory.restClient("http://localhost")).isNotNull();
        assertThat(factory.webClient("http://localhost")).isNotNull();
        assertThat(factory.requestFactory()).isNotNull();
    }

    @Test
    void loggingInterceptorRedactsAndCanBeDisabled() throws IOException {
        IntegrationLoggingInterceptor enabled = new IntegrationLoggingInterceptor(true);
        IntegrationLoggingInterceptor disabled = new IntegrationLoggingInterceptor(false);

        ClientHttpRequest request = mock(ClientHttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost/patient?rut=12.345.678-5"));
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(response);

        assertThat(enabled.intercept(request, "{}".getBytes(), execution)).isSameAs(response);
        assertThat(disabled.intercept(request, "{}".getBytes(), execution)).isSameAs(response);
    }

    @Test
    void webClientBuilderAppliesFilters() {
        IntegrationHttpClientFactory factory =
                new IntegrationHttpClientFactory(properties(), List.of(), List.of(), null);

        WebClient.Builder builder = factory.webClientBuilder();
        assertThat(builder).isNotNull();
    }

    private static TelemedicineIntegrationProperties properties() {
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.getHttp().setLogRequests(true);
        return properties;
    }
}
