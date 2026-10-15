package com.telemedicine.common.security.propagation;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * {@link ClaimPropagationInterceptor} for {@code WebClient}.
 *
 * <p>Values are captured when the request is assembled, which on a servlet stack is still the
 * caller's thread and therefore still has the security context and MDC. Reading them inside the
 * reactive pipeline instead would find an empty context on whichever scheduler happened to run.
 */
public class ClaimPropagationExchangeFilter implements ExchangeFilterFunction {

    private final SecurityContextAccessor securityContext;
    private final TelemedicineSecurityProperties.Propagation properties;

    public ClaimPropagationExchangeFilter(
            SecurityContextAccessor securityContext,
            TelemedicineSecurityProperties.Propagation properties) {
        this.securityContext = securityContext;
        this.properties =
                properties == null ? new TelemedicineSecurityProperties.Propagation() : properties;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String token =
                properties.isForwardAuthorization()
                        ? securityContext.currentTokenValue().orElse(null)
                        : null;
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        String tenantId = MDC.get(MdcKeys.TENANT_ID);

        ClientRequest.Builder builder = ClientRequest.from(request);
        if (token != null && !request.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (correlationId != null
                && !correlationId.isBlank()
                && !request.headers().containsKey(Headers.CORRELATION_ID)) {
            builder.header(Headers.CORRELATION_ID, correlationId);
        }
        if (tenantId != null
                && !tenantId.isBlank()
                && !request.headers().containsKey(Headers.TENANT_ID)) {
            builder.header(Headers.TENANT_ID, tenantId);
        }
        return next.exchange(builder.build());
    }
}
