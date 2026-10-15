package com.telemedicine.gateway.filter;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.id.IdGenerator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(Headers.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = IdGenerator.correlationId();
        }
        String finalCorrelationId = correlationId;
        ServerHttpRequest decorated =
                new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.putAll(super.getHeaders());
                        headers.set(Headers.CORRELATION_ID, finalCorrelationId);
                        return HttpHeaders.readOnlyHttpHeaders(headers);
                    }
                };
        ServerWebExchange mutated = exchange.mutate().request(decorated).build();
        try {
            mutated.getResponse().getHeaders().set(Headers.CORRELATION_ID, finalCorrelationId);
        } catch (UnsupportedOperationException ignored) {
            // Response headers are frozen after Netty starts writing.
        }
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
