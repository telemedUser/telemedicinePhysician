package com.telemedicine.common.core.error;

import java.util.Map;

/** The caller is authenticated but lacks the role, scope or ownership the operation requires. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode code, String message) {
        super(code, message);
    }

    public ForbiddenException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    public static ForbiddenException missingRole(String requiredRole) {
        return new ForbiddenException(
                ErrorCode.INSUFFICIENT_ROLE,
                "Caller lacks the required role",
                Map.of("requiredRole", requiredRole));
    }
}
