package com.telemedicine.common.observability.filter;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import com.telemedicine.common.observability.mdc.MdcSupport;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Accepts the inbound correlation header set by the gateway, generates one when absent, echoes it
 * on the response and populates the standard MDC keys for structured logging.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private final TelemedicineObservabilityProperties properties;
    private final Tracer tracer;

    public CorrelationIdFilter(TelemedicineObservabilityProperties properties, Tracer tracer) {
        this.properties = properties;
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        MdcSupport.populateFromRequest(httpRequest, properties, tracer);
        String correlationId = org.slf4j.MDC.get(MdcKeys.CORRELATION_ID);
        httpResponse.setHeader(Headers.CORRELATION_ID, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MdcSupport.clearRequestScope();
        }
    }
}
