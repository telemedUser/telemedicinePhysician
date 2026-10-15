package com.telemedicine.common.integration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

class SimScenarioPropagationInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.remove(Headers.SIM_SCENARIO);
    }

    @Test
    void forwardsScenarioFromMdc() throws IOException {
        SimScenarioPropagationInterceptor interceptor = new SimScenarioPropagationInterceptor();
        ClientHttpRequest request = mock(ClientHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        MDC.put(Headers.SIM_SCENARIO, "timeout");
        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(Headers.SIM_SCENARIO)).isEqualTo("timeout");
        verify(execution).execute(request, new byte[0]);
    }
}
