package com.telemedicine.common.security.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

class ClaimPropagationExchangeFilterTest {

    private final TelemedicineSecurityProperties.Propagation properties =
            new TelemedicineSecurityProperties().getPropagation();
    private final SecurityContextAccessor securityContext =
            new SecurityContextAccessor(new TelemedicineSecurityProperties().getJwt());

    @AfterEach
    void clearState() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void forwardsIdentityAndCorrelation() {
        authenticate();
        MDC.put(MdcKeys.CORRELATION_ID, "01JCORR");
        MDC.put(MdcKeys.TENANT_ID, "bupa-cl");

        HttpHeaders headers = exchange(request(builder -> builder));

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer caller-token");
        assertThat(headers.getFirst(Headers.CORRELATION_ID)).isEqualTo("01JCORR");
        assertThat(headers.getFirst(Headers.TENANT_ID)).isEqualTo("bupa-cl");
    }

    @Test
    void neverOverwritesHeadersTheCallerSet() {
        authenticate();
        MDC.put(MdcKeys.CORRELATION_ID, "01JCORR");
        MDC.put(MdcKeys.TENANT_ID, "bupa-cl");

        HttpHeaders headers =
                exchange(
                        request(
                                builder ->
                                        builder.header(HttpHeaders.AUTHORIZATION, "Basic vendor")
                                                .header(Headers.CORRELATION_ID, "explicit")
                                                .header(Headers.TENANT_ID, "other")));

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Basic vendor");
        assertThat(headers.getFirst(Headers.CORRELATION_ID)).isEqualTo("explicit");
        assertThat(headers.getFirst(Headers.TENANT_ID)).isEqualTo("other");
    }

    @Test
    void addsNothingWhenThereIsNoContextToPropagate() {
        HttpHeaders headers = exchange(request(builder -> builder));

        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.containsKey(Headers.CORRELATION_ID)).isFalse();
        assertThat(headers.containsKey(Headers.TENANT_ID)).isFalse();
    }

    @Test
    @DisplayName("blank MDC values are not propagated as empty headers")
    void ignoresBlankValues() {
        MDC.put(MdcKeys.CORRELATION_ID, "  ");
        MDC.put(MdcKeys.TENANT_ID, "  ");

        HttpHeaders headers = exchange(request(builder -> builder));

        assertThat(headers.containsKey(Headers.CORRELATION_ID)).isFalse();
        assertThat(headers.containsKey(Headers.TENANT_ID)).isFalse();
    }

    @Test
    void skipsTheTokenWhenForwardingIsTurnedOff() {
        authenticate();
        properties.setForwardAuthorization(false);

        assertThat(exchange(request(builder -> builder)).containsKey(HttpHeaders.AUTHORIZATION))
                .isFalse();
    }

    @Test
    void fallsBackToDefaultsWhenConstructedWithoutConfiguration() {
        authenticate();

        ClaimPropagationExchangeFilter filter =
                new ClaimPropagationExchangeFilter(securityContext, null);
        RecordingExchange exchange = new RecordingExchange();
        filter.filter(request(builder -> builder), exchange).block();

        assertThat(exchange.headers.getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer caller-token");
    }

    private HttpHeaders exchange(ClientRequest request) {
        RecordingExchange exchange = new RecordingExchange();
        new ClaimPropagationExchangeFilter(securityContext, properties)
                .filter(request, exchange)
                .block();
        return exchange.headers;
    }

    private static ClientRequest request(
            java.util.function.UnaryOperator<ClientRequest.Builder> customizer) {
        return customizer
                .apply(
                        ClientRequest.create(
                                HttpMethod.GET, URI.create("http://config-service/v1/x")))
                .build();
    }

    private static void authenticate() {
        Jwt jwt =
                Jwt.withTokenValue("caller-token")
                        .header("alg", "RS256")
                        .subject("2b3c4d5e")
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    static class RecordingExchange implements ExchangeFunction {

        HttpHeaders headers;

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            headers = new HttpHeaders();
            headers.addAll(request.headers());
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        }
    }
}
