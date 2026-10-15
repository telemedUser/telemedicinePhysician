package com.telemedicine.common.core.error;

import com.telemedicine.common.core.dto.Violation;
import java.util.List;
import java.util.Map;

/** The request was well-formed but failed business or bean validation. */
public class ValidationException extends DomainException {

    private final transient List<Violation> violations;

    public ValidationException(String message) {
        this(message, List.of());
    }

    public ValidationException(String message, List<Violation> violations) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public ValidationException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public ValidationException(ErrorCode code, String message, List<Violation> violations) {
        super(code, message);
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public ValidationException(String message, Map<String, Object> details) {
        super(ErrorCode.VALIDATION_ERROR, message, details);
        this.violations = List.of();
    }

    /** Immutable, never null. */
    public List<Violation> getViolations() {
        return violations;
    }

    public static ValidationException field(String field, String message) {
        return new ValidationException(
                "Request validation failed", List.of(Violation.of(field, message)));
    }
}
