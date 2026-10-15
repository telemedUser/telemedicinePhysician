package com.telemedicine.common.core.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base type for every expected business failure.
 *
 * <p>Throwing this (or a subclass) is how a service says "this is a defined outcome, translate it
 * to the shared error contract" rather than "something broke". The web layer maps it onto an RFC
 * 9457 problem document using {@link ErrorCode#httpStatus()}.
 *
 * <p>The {@code details} map is rendered into the problem document as extension members, so it must
 * never carry PHI in the clear.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode code;
    private final transient Map<String, Object> details;
    private final String correlationId;

    public DomainException(ErrorCode code, String message) {
        this(code, message, Map.of(), null, null);
    }

    public DomainException(ErrorCode code, String message, Throwable cause) {
        this(code, message, Map.of(), null, cause);
    }

    public DomainException(ErrorCode code, String message, Map<String, Object> details) {
        this(code, message, details, null, null);
    }

    public DomainException(
            ErrorCode code, String message, Map<String, Object> details, Throwable cause) {
        this(code, message, details, null, cause);
    }

    protected DomainException(
            ErrorCode code,
            String message,
            Map<String, Object> details,
            String correlationId,
            Throwable cause) {
        super(message, cause);
        if (code == null) {
            throw new IllegalArgumentException("ErrorCode is required");
        }
        this.code = code;
        this.details =
                details == null || details.isEmpty()
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(details));
        this.correlationId = correlationId;
    }

    public ErrorCode getCode() {
        return code;
    }

    /** Immutable, never null. Rendered as problem-document extension members. */
    public Map<String, Object> getDetails() {
        return details;
    }

    /** Correlation ID captured at throw time, or null when the caller did not supply one. */
    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isRetryable() {
        return code.retryable();
    }

    /** Returns a copy carrying the supplied correlation ID; this instance is left unchanged. */
    public DomainException withCorrelationId(String newCorrelationId) {
        return new DomainException(code, getMessage(), details, newCorrelationId, getCause());
    }
}
