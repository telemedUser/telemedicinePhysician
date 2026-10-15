package com.telemedicine.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.constants.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CorrelationIdGatewayFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private CorrelationIdGatewayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdGatewayFilter();
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void echoesProvidedCorrelationId() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/actuator/health")
                        .header(Headers.CORRELATION_ID, "test-correlation-123")
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(Headers.CORRELATION_ID))
                .isEqualTo("test-correlation-123");
        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst(Headers.CORRELATION_ID))
                .isEqualTo("test-correlation-123");
    }

    @Test
    void generatesCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        String header = exchange.getResponse().getHeaders().getFirst(Headers.CORRELATION_ID);
        assertThat(header).isNotBlank();
    }
}
