package com.telemedicine.common.core.error;

import java.util.Map;

/** The request conflicts with the current state of the resource. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }

    public ConflictException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    /**
     * Raised when a state machine rejects a transition, for example OPEN_DOCTOR before CHECKED_IN.
     */
    public static ConflictException invalidTransition(String from, String to) {
        return new ConflictException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "Transition from " + from + " to " + to + " is not allowed",
                Map.of("from", from, "to", to));
    }
}
