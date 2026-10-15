package com.telemedicine.common.web;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.dto.ProblemDetail;
import com.telemedicine.common.core.dto.ProblemDetails;
import com.telemedicine.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Turns a catalogue error into an HTTP response.
 *
 * <p>Centralising this is what guarantees every service answers with the same media type, the same
 * extension members and the same echoed correlation header. Support runbooks tell L2 to trace by
 * {@code X-Correlation-Id}, which only works if the header is present on failures too.
 */
public final class ProblemResponses {

    private ProblemResponses() {}

    /** Builds a problem document seeded with correlation, trace, timestamp and request path. */
    public static ProblemDetails builder(ErrorCode code, HttpServletRequest request) {
        return enrich(ProblemDetails.of(code), request);
    }

    /**
     * Variant for statuses raised outside the error catalogue, such as a bare {@code
     * ResponseStatusException}. Deliberately omits the {@code category} and {@code retryable}
     * extensions, which would otherwise assert something untrue.
     */
    public static ProblemDetails statusBuilder(
            int status, String title, HttpServletRequest request) {
        return enrich(ProblemDetails.ofStatus(status, title), request);
    }

    private static ProblemDetails enrich(ProblemDetails builder, HttpServletRequest request) {
        builder.timestamp(Instant.now()).instance(pathOf(request));
        String correlationId = correlationId(request);
        if (correlationId != null) {
            builder.correlationId(correlationId);
        }
        String traceId = MDC.get(MdcKeys.TRACE_ID);
        if (traceId != null) {
            builder.traceId(traceId);
        }
        return builder;
    }

    /** Renders the document as {@code application/problem+json} with the correlation header. */
    public static ResponseEntity<ProblemDetail> render(
            ProblemDetail problem, HttpServletRequest request) {
        ResponseEntity.BodyBuilder response =
                ResponseEntity.status(problem.status())
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        String correlationId = correlationId(request);
        if (correlationId != null) {
            response.header(Headers.CORRELATION_ID, correlationId);
        }
        return response.body(problem);
    }

    /**
     * Correlation id for the current request: the MDC value set by the observability filter, or the
     * inbound header when this module is used without that filter.
     */
    public static String correlationId(HttpServletRequest request) {
        String fromMdc = MDC.get(MdcKeys.CORRELATION_ID);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        return request == null ? null : request.getHeader(Headers.CORRELATION_ID);
    }

    private static String pathOf(HttpServletRequest request) {
        return request == null ? null : request.getRequestURI();
    }
}
