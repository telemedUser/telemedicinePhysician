package com.telemedicine.common.core.error;

import java.util.Map;

/** The addressed resource does not exist, or the caller may not learn that it exists. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }

    /**
     * Convenience for the common "entity by id" case. The identifier is echoed back as a detail, so
     * callers must not pass a raw patient identifier here.
     */
    public static NotFoundException of(String resourceType, String resourceId) {
        return new NotFoundException(
                ErrorCode.NOT_FOUND,
                resourceType + " not found",
                Map.of("resourceType", resourceType, "resourceId", resourceId));
    }

    public NotFoundException(ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
