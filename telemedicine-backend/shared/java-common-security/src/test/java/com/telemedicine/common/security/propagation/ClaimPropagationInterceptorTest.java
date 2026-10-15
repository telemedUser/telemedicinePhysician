package com.telemedicine.common.security.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ClaimPropagationInterceptorTest {

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
    @DisplayName("the caller's token and correlation id travel with the downstream call")
    void forwardsIdentityAndCorrelation() throws IOException {
        authenticate("caller-token");
        MDC.put(MdcKeys.CORRELATION_ID, "01JCORR");
        MDC.put(MdcKeys.TENANT_ID, "bupa-cl");

        HttpHeaders headers = intercept();

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer caller-token");
        assertThat(headers.getFirst(Headers.CORRELATION_ID)).isEqualTo("01JCORR");
        assertThat(headers.getFirst(Headers.TENANT_ID)).isEqualTo("bupa-cl");
    }

    @Test
    @DisplayName("an adapter calling a third party with its own credentials keeps them")
    void neverOverwritesHeadersTheCallerSet() throws IOException {
        authenticate("caller-token");
        MDC.put(MdcKeys.CORRELATION_ID, "01JCORR");

        HttpHeaders headers =
                intercept(
                        request -> {
                            request.getHeaders()
                                    .set(HttpHeaders.AUTHORIZATION, "Basic vendor-secret");
                            request.getHeaders().set(Headers.CORRELATION_ID, "explicit");
                        });

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Basic vendor-secret");
        assertThat(headers.getFirst(Headers.CORRELATION_ID)).isEqualTo("explicit");
    }

    @Test
    void addsNothingWhenThereIsNoContextToPropagate() throws IOException {
        HttpHeaders headers = intercept();

        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.containsKey(Headers.CORRELATION_ID)).isFalse();
    }

    @Test
    void skipsTheTokenWhenForwardingIsTurnedOff() throws IOException {
        authenticate("caller-token");
        MDC.put(MdcKeys.CORRELATION_ID, "01JCORR");
        properties.setForwardAuthorization(false);

        HttpHeaders headers = intercept();

        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.getFirst(Headers.CORRELATION_ID)).isEqualTo("01JCORR");
    }

    @Test
    void ignoresABlankMdcValue() throws IOException {
        MDC.put(MdcKeys.CORRELATION_ID, "   ");

        assertThat(intercept().containsKey(Headers.CORRELATION_ID)).isFalse();
    }

    @Test
    void fallsBackToDefaultsWhenConstructedWithoutConfiguration() throws IOException {
        authenticate("caller-token");

        HttpHeaders headers =
                execute(new ClaimPropagationInterceptor(securityContext, null), request -> {});

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer caller-token");
    }

    private HttpHeaders intercept() throws IOException {
        return intercept(request -> {});
    }

    private HttpHeaders intercept(java.util.function.Consumer<HttpRequest> customizer)
            throws IOException {
        return execute(new ClaimPropagationInterceptor(securityContext, properties), customizer);
    }

    private static HttpHeaders execute(
            ClaimPropagationInterceptor interceptor,
            java.util.function.Consumer<HttpRequest> customizer)
            throws IOException {
        MockClientHttpRequest request =
                new MockClientHttpRequest(
                        HttpMethod.POST, URI.create("http://config-service/v1/x"));
        customizer.accept(request);
        RecordingExecution execution = new RecordingExecution();
        interceptor.intercept(request, new byte[0], execution);
        return execution.headers;
    }

    private static void authenticate(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue).header("alg", "RS256").subject("2b3c4d5e").build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    static class RecordingExecution implements ClientHttpRequestExecution {

        HttpHeaders headers;

        @Override
        public ClientHttpResponse execute(HttpRequest request, byte[] body) {
            headers = HttpHeaders.readOnlyHttpHeaders(request.getHeaders());
            return new MockClientHttpResponse(new byte[0], 200);
        }
    }
}
