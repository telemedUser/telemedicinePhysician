package com.telemedicine.common.core.dto;

import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.phi.PhiRedactor;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for RFC 9457 problem documents.
 *
 * <p>{@link ProblemDetail} keeps its original seven-member shape for binary compatibility, so
 * everything the RFC calls an extension member — correlation id, trace id, timestamp, field
 * violations — is carried in its {@code extensions} map. This builder is the only supported way to
 * populate them, which keeps extension key names consistent across all seventeen services.
 *
 * <p>{@code detail} is passed through {@link PhiRedactor} on the way in. Problem documents are
 * returned to browsers and written to access logs, and the most common PHI leak in a REST API is an
 * exception message that quotes the offending input back to the caller.
 */
public final class ProblemDetails {

    /** Extension member keys. Stable: dashboards and the front-end error mapper depend on them. */
    public static final String EXT_CORRELATION_ID = "correlationId";

    public static final String EXT_TRACE_ID = "traceId";
    public static final String EXT_TIMESTAMP = "timestamp";
    public static final String EXT_VIOLATIONS = "violations";
    public static final String EXT_RETRYABLE = "retryable";
    public static final String EXT_CATEGORY = "category";

    private URI type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private String code;
    private final Map<String, Object> extensions = new LinkedHashMap<>();
    private final List<Violation> violations = new ArrayList<>();

    private ProblemDetails() {}

    /** Seeds title, status, type, code, category and retryability from the catalogue entry. */
    public static ProblemDetails of(ErrorCode errorCode) {
        ProblemDetails builder = new ProblemDetails();
        builder.type = errorCode.problemType();
        builder.title = errorCode.title();
        builder.status = errorCode.httpStatus();
        builder.code = errorCode.code();
        builder.extensions.put(EXT_CATEGORY, errorCode.category().name());
        builder.extensions.put(EXT_RETRYABLE, errorCode.retryable());
        return builder;
    }

    /** Seeds from a thrown domain exception, including its detail map and correlation id. */
    public static ProblemDetails from(DomainException exception) {
        ProblemDetails builder = of(exception.getCode()).detail(exception.getMessage());
        exception.getDetails().forEach(builder::extension);
        if (exception.getCorrelationId() != null) {
            builder.correlationId(exception.getCorrelationId());
        }
        return builder;
    }

    /** Escape hatch for statuses with no catalogue entry, such as a raw container-level 404. */
    public static ProblemDetails ofStatus(int status, String title) {
        ProblemDetails builder = new ProblemDetails();
        builder.type = URI.create("about:blank");
        builder.title = title;
        builder.status = status;
        return builder;
    }

    public ProblemDetails detail(String value) {
        this.detail = PhiRedactor.redact(value);
        return this;
    }

    /** Request path the problem applies to. */
    public ProblemDetails instance(String value) {
        this.instance = value;
        return this;
    }

    public ProblemDetails type(URI value) {
        this.type = value;
        return this;
    }

    public ProblemDetails title(String value) {
        this.title = value;
        return this;
    }

    public ProblemDetails status(int value) {
        this.status = value;
        return this;
    }

    public ProblemDetails code(String value) {
        this.code = value;
        return this;
    }

    public ProblemDetails correlationId(String value) {
        return extension(EXT_CORRELATION_ID, value);
    }

    public ProblemDetails traceId(String value) {
        return extension(EXT_TRACE_ID, value);
    }

    public ProblemDetails timestamp(Instant value) {
        return extension(EXT_TIMESTAMP, value == null ? null : value.toString());
    }

    public ProblemDetails violation(Violation value) {
        if (value != null) {
            violations.add(value);
        }
        return this;
    }

    public ProblemDetails violations(List<Violation> values) {
        if (values != null) {
            violations.addAll(values);
        }
        return this;
    }

    /** Adds an arbitrary extension member. Null values are dropped rather than serialised. */
    public ProblemDetails extension(String key, Object value) {
        if (key != null && value != null) {
            extensions.put(key, value);
        }
        return this;
    }

    public ProblemDetail build() {
        Map<String, Object> members = new LinkedHashMap<>(extensions);
        if (!violations.isEmpty()) {
            members.put(EXT_VIOLATIONS, List.copyOf(violations));
        }
        return new ProblemDetail(
                type == null ? URI.create("about:blank") : type,
                title,
                status,
                detail,
                instance,
                code,
                Map.copyOf(members));
    }
}
