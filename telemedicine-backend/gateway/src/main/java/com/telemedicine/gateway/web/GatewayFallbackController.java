package com.telemedicine.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.dto.ProblemDetail;
import com.telemedicine.common.core.dto.ProblemDetails;
import com.telemedicine.common.core.error.ErrorCode;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class GatewayFallbackController {

    private final ObjectMapper objectMapper;

    public GatewayFallbackController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<byte[]>> fallback(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(Headers.CORRELATION_ID);
        ProblemDetail problem =
                ProblemDetails.of(ErrorCode.CIRCUIT_OPEN)
                        .detail("Upstream service unavailable or circuit open")
                        .correlationId(correlationId)
                        .timestamp(Instant.now())
                        .instance(exchange.getRequest().getPath().value())
                        .build();
        try {
            byte[] body = objectMapper.writeValueAsBytes(problem);
            return Mono.just(
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                            .header(Headers.CORRELATION_ID, correlationId == null ? "" : correlationId)
                            .body(body));
        } catch (Exception exception) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
    }
}
