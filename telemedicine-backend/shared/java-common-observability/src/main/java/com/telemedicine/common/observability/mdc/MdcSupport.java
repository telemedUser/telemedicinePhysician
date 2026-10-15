package com.telemedicine.common.observability.mdc;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Populates and clears the standard Telemedicine MDC keys for a request scope. */
public final class MdcSupport {

    private MdcSupport() {}

    public static void populateFromRequest(
            HttpServletRequest request,
            TelemedicineObservabilityProperties properties,
            Tracer tracer) {
        String correlationId =
                headerOrGenerate(request, Headers.CORRELATION_ID, IdGenerator::correlationId);
        MDC.put(MdcKeys.CORRELATION_ID, correlationId);

        String requestId = headerOrGenerate(request, Headers.REQUEST_ID, IdGenerator::newUlid);
        MDC.put(MdcKeys.REQUEST_ID, requestId);

        String causationId = request.getHeader(Headers.CAUSATION_ID);
        if (causationId != null && !causationId.isBlank()) {
            MDC.put(MdcKeys.CAUSATION_ID, causationId);
        }

        String tenantId = request.getHeader(Headers.TENANT_ID);
        if (tenantId != null && !tenantId.isBlank()) {
            MDC.put(MdcKeys.TENANT_ID, tenantId);
        }

        String appointmentId =
                request.getHeader(properties.getCorrelation().getAppointmentHeader());
        if (appointmentId != null && !appointmentId.isBlank()) {
            MDC.put(MdcKeys.APPOINTMENT_ID, appointmentId);
        }

        populateTracing(tracer);
        populateSecurityContext();
    }

    public static void populateTracing(Tracer tracer) {
        if (tracer == null) {
            return;
        }
        Span current = tracer.currentSpan();
        if (current == null) {
            return;
        }
        MDC.put(MdcKeys.TRACE_ID, current.context().traceId());
        MDC.put(MdcKeys.SPAN_ID, current.context().spanId());
    }

    public static void populateSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
            MDC.put(MdcKeys.USER_ID, name);
        }
        authentication.getAuthorities().stream()
                .map(Object::toString)
                .findFirst()
                .ifPresent(role -> MDC.put(MdcKeys.USER_ROLE, role));
    }

    public static void clearRequestScope() {
        MDC.remove(MdcKeys.CORRELATION_ID);
        MDC.remove(MdcKeys.CAUSATION_ID);
        MDC.remove(MdcKeys.REQUEST_ID);
        MDC.remove(MdcKeys.TRACE_ID);
        MDC.remove(MdcKeys.SPAN_ID);
        MDC.remove(MdcKeys.USER_ID);
        MDC.remove(MdcKeys.USER_ROLE);
        MDC.remove(MdcKeys.TENANT_ID);
        MDC.remove(MdcKeys.APPOINTMENT_ID);
        MDC.remove(MdcKeys.SESSION_ID);
        MDC.remove(MdcKeys.EVENT_ID);
        MDC.remove(MdcKeys.EVENT_TYPE);
    }

    public static void putEventContext(String eventId, String eventType) {
        if (eventId != null && !eventId.isBlank()) {
            MDC.put(MdcKeys.EVENT_ID, eventId);
        }
        if (eventType != null && !eventType.isBlank()) {
            MDC.put(MdcKeys.EVENT_TYPE, eventType);
        }
    }

    public static void clearEventContext() {
        MDC.remove(MdcKeys.EVENT_ID);
        MDC.remove(MdcKeys.EVENT_TYPE);
    }

    private static String headerOrGenerate(
            HttpServletRequest request,
            String header,
            java.util.function.Supplier<String> generator) {
        return Optional.ofNullable(request.getHeader(header))
                .filter(value -> !value.isBlank())
                .orElseGet(generator);
    }
}
