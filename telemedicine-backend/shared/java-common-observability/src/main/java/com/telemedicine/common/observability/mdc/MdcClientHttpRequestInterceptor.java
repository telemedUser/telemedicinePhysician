package com.telemedicine.common.observability.mdc;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Forwards correlation headers from MDC on outbound {@link
 * org.springframework.web.client.RestClient} calls.
 */
public class MdcClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        forward(request, Headers.CORRELATION_ID, MDC.get(MdcKeys.CORRELATION_ID));
        forward(request, Headers.CAUSATION_ID, MDC.get(MdcKeys.CAUSATION_ID));
        forward(request, Headers.TENANT_ID, MDC.get(MdcKeys.TENANT_ID));
        forward(request, Headers.REQUEST_ID, MDC.get(MdcKeys.REQUEST_ID));
        forward(request, Headers.TRACEPARENT, MDC.get(MdcKeys.TRACE_ID));
        return execution.execute(request, body);
    }

    private static void forward(HttpRequest request, String header, String value) {
        if (value != null && !value.isBlank() && !request.getHeaders().containsKey(header)) {
            request.getHeaders().set(header, value);
        }
    }
}
