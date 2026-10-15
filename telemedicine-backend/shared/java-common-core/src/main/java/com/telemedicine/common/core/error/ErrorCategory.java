package com.telemedicine.common.core.error;

/**
 * Coarse grouping used for metrics, alert routing and audit classification. Finer detail lives in
 * {@link ErrorCode}.
 */
public enum ErrorCategory {
    /** The request could not be understood or failed bean validation. */
    VALIDATION,
    /** The caller could not be authenticated. */
    AUTHENTICATION,
    /** The caller was authenticated but lacks the required role or scope. */
    AUTHORIZATION,
    /** The addressed resource does not exist. */
    NOT_FOUND,
    /** The request conflicts with the current state of the resource. */
    CONFLICT,
    /** Platform terms or clinical informed consent are missing or withdrawn. */
    CONSENT,
    /** Patient identity or FONASA two-factor validation failed. */
    IDENTITY,
    /** Payment has not been confirmed by the external payment platform. */
    PAYMENT,
    /** An external enterprise system rejected or failed the call. */
    INTEGRATION,
    /** A pluggable provider (video, telemetry, prescription) is unavailable. */
    PROVIDER,
    /** The caller exceeded a rate limit or quota. */
    RATE_LIMIT,
    /** An unexpected fault inside the platform. */
    INTERNAL
}
