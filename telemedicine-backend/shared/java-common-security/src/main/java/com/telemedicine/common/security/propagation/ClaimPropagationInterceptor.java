package com.telemedicine.common.security.propagation;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Forwards the caller's identity and correlation context on outbound service calls.
 *
 * <p>A downstream service that receives no bearer token has to be configured to trust its network,
 * and a downstream call that starts a fresh correlation id breaks the end-to-end trace support
 * relies on. Both are prevented here, once, rather than per call site.
 *
 * <p>Headers already set on the outbound request are never overwritten, so an adapter deliberately
 * calling a third party with its own credentials keeps them.
 */
public class ClaimPropagationInterceptor implements ClientHttpRequestInterceptor {

    private final SecurityContextAccessor securityContext;
    private final TelemedicineSecurityProperties.Propagation properties;

    public ClaimPropagationInterceptor(
            SecurityContextAccessor securityContext,
            TelemedicineSecurityProperties.Propagation properties) {
        this.securityContext = securityContext;
        this.properties =
                properties == null ? new TelemedicineSecurityProperties.Propagation() : properties;
    }

    @Override
    public ClientHttpResponse intercept(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution)
            throws IOException {
        HttpHeaders headers = request.getHeaders();
        if (properties.isForwardAuthorization()
                && !headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            securityContext
                    .currentTokenValue()
                    .ifPresent(token -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        }
        copyFromMdc(headers, Headers.CORRELATION_ID, MdcKeys.CORRELATION_ID);
        copyFromMdc(headers, Headers.TENANT_ID, MdcKeys.TENANT_ID);
        return execution.execute(request, body);
    }

    private static void copyFromMdc(HttpHeaders headers, String headerName, String mdcKey) {
        if (headers.containsKey(headerName)) {
            return;
        }
        String value = MDC.get(mdcKey);
        if (value != null && !value.isBlank()) {
            headers.set(headerName, value);
        }
    }
}
