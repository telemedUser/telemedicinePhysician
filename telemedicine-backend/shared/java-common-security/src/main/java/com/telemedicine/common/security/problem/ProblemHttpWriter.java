package com.telemedicine.common.security.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.dto.ProblemDetail;
import com.telemedicine.common.core.dto.ProblemDetails;
import com.telemedicine.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.MDC;

/**
 * Writes an RFC 9457 problem document straight to the servlet response.
 *
 * <p>Authentication and authorisation failures are rejected by the filter chain, before Spring MVC
 * and therefore before {@code @RestControllerAdvice} can see them. Without this, a service that
 * answers every business error with {@code application/problem+json} answers 401 and 403 with
 * Spring's default empty body, and clients need two error parsers.
 *
 * <p>This module does not depend on {@code java-common-web}: the problem model lives in {@code
 * java-common-core} precisely so both layers can render the same contract independently.
 */
public final class ProblemHttpWriter {

    private final ObjectMapper objectMapper;

    public ProblemHttpWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /** Renders the catalogue error, echoing the correlation id so failures stay traceable. */
    public void write(
            HttpServletRequest request, HttpServletResponse response, ErrorCode code, String detail)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ProblemDetails builder = ProblemDetails.of(code).detail(detail).timestamp(Instant.now());
        if (request != null) {
            builder.instance(request.getRequestURI());
        }
        String correlationId = correlationId(request);
        if (correlationId != null) {
            builder.correlationId(correlationId);
            response.setHeader(Headers.CORRELATION_ID, correlationId);
        }
        String traceId = MDC.get(MdcKeys.TRACE_ID);
        if (traceId != null) {
            builder.traceId(traceId);
        }
        ProblemDetail problem = builder.build();
        response.setStatus(problem.status());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private static String correlationId(HttpServletRequest request) {
        String fromMdc = MDC.get(MdcKeys.CORRELATION_ID);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        return request == null ? null : request.getHeader(Headers.CORRELATION_ID);
    }
}
