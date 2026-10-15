package com.telemedicine.common.integration.client;

import com.telemedicine.common.core.phi.PhiRedactor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** PHI-redacted request/response logging for outbound RestClient calls. */
public class IntegrationLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IntegrationLoggingInterceptor.class);

    private final boolean enabled;

    public IntegrationLoggingInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (enabled) {
            log.debug(
                    "Outbound HTTP {} {} body={}",
                    request.getMethod(),
                    PhiRedactor.redact(request.getURI().toString()),
                    PhiRedactor.redact(new String(body, StandardCharsets.UTF_8)));
        }
        ClientHttpResponse response = execution.execute(request, body);
        if (enabled) {
            log.debug(
                    "Outbound HTTP {} {} -> {}",
                    request.getMethod(),
                    PhiRedactor.redact(request.getURI().toString()),
                    response.getStatusCode().value());
        }
        return response;
    }
}
