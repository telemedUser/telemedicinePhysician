package com.telemedicine.common.core.error;

import java.net.URI;
import java.util.Locale;

/**
 * The platform-wide error catalogue.
 *
 * <p>Every constant carries a stable wire code that is safe to publish in API contracts and to
 * match on in client code. The enum name may never be reused for a different meaning, and the
 * {@link #code()} string is part of the public contract: renaming one is a breaking change.
 *
 * <p>The first ten constants predate the catalogue and keep their original names.
 */
public enum ErrorCode {

    // --- Generic request and platform faults ---
    VALIDATION_ERROR("TM-VAL-001", 400, ErrorCategory.VALIDATION, false),
    NOT_FOUND("TM-NFD-001", 404, ErrorCategory.NOT_FOUND, false),
    UNAUTHORIZED("TM-AUT-001", 401, ErrorCategory.AUTHENTICATION, false),
    FORBIDDEN("TM-AUZ-001", 403, ErrorCategory.AUTHORIZATION, false),
    CONFLICT("TM-CFL-001", 409, ErrorCategory.CONFLICT, false),
    INTEGRATION_ERROR("TM-ITG-001", 502, ErrorCategory.INTEGRATION, true),
    PROVIDER_OUTAGE("TM-PRV-001", 503, ErrorCategory.PROVIDER, true),
    CONSENT_REQUIRED("TM-CNS-001", 403, ErrorCategory.CONSENT, false),
    IDENTITY_VALIDATION_FAILED("TM-IDN-001", 422, ErrorCategory.IDENTITY, false),
    INTERNAL_ERROR("TM-INT-001", 500, ErrorCategory.INTERNAL, false),

    // --- Request shape ---
    MALFORMED_REQUEST("TM-VAL-002", 400, ErrorCategory.VALIDATION, false),
    UNSUPPORTED_MEDIA_TYPE("TM-VAL-003", 415, ErrorCategory.VALIDATION, false),
    METHOD_NOT_ALLOWED("TM-VAL-004", 405, ErrorCategory.VALIDATION, false),
    PAYLOAD_TOO_LARGE("TM-VAL-005", 413, ErrorCategory.VALIDATION, false),
    MISSING_PARAMETER("TM-VAL-006", 400, ErrorCategory.VALIDATION, false),

    // --- Concurrency and idempotency ---
    OPTIMISTIC_LOCK_CONFLICT("TM-CFL-002", 409, ErrorCategory.CONFLICT, true),
    IDEMPOTENCY_KEY_REQUIRED("TM-CFL-003", 400, ErrorCategory.VALIDATION, false),
    IDEMPOTENCY_KEY_CONFLICT("TM-CFL-004", 409, ErrorCategory.CONFLICT, false),

    // --- Authentication and authorisation ---
    TOKEN_EXPIRED("TM-AUT-002", 401, ErrorCategory.AUTHENTICATION, false),
    INVALID_TOKEN("TM-AUT-003", 401, ErrorCategory.AUTHENTICATION, false),
    INSUFFICIENT_ROLE("TM-AUZ-002", 403, ErrorCategory.AUTHORIZATION, false),
    WEBHOOK_SIGNATURE_INVALID("TM-AUT-004", 401, ErrorCategory.AUTHENTICATION, false),

    // --- Rate limiting ---
    RATE_LIMITED("TM-RTL-001", 429, ErrorCategory.RATE_LIMIT, true),

    // --- Consent ---
    CONSENT_WITHDRAWN("TM-CNS-002", 403, ErrorCategory.CONSENT, false),
    CONSENT_VERSION_OUTDATED("TM-CNS-003", 409, ErrorCategory.CONSENT, false),

    // --- Identity and FONASA two-factor ---
    IDENTITY_2FA_REQUIRED("TM-IDN-002", 428, ErrorCategory.IDENTITY, false),
    IDENTITY_2FA_INVALID("TM-IDN-003", 422, ErrorCategory.IDENTITY, false),
    IDENTITY_LOCKED_OUT("TM-IDN-004", 423, ErrorCategory.IDENTITY, false),
    MPI_PATIENT_NOT_FOUND("TM-IDN-005", 404, ErrorCategory.IDENTITY, false),

    // --- Appointment, payment and secure link ---
    PAYMENT_NOT_CONFIRMED("TM-PAY-001", 402, ErrorCategory.PAYMENT, false),
    PAYMENT_VALIDATION_FAILED("TM-PAY-002", 402, ErrorCategory.PAYMENT, true),
    APPOINTMENT_NOT_FOUND("TM-APT-001", 404, ErrorCategory.NOT_FOUND, false),
    APPOINTMENT_NOT_ELIGIBLE("TM-APT-002", 409, ErrorCategory.CONFLICT, false),
    SESSION_LINK_EXPIRED("TM-LNK-001", 410, ErrorCategory.CONFLICT, false),
    SESSION_LINK_ALREADY_USED("TM-LNK-002", 409, ErrorCategory.CONFLICT, false),

    // --- Waiting room ---
    DEVICE_TEST_REQUIRED("TM-WTR-001", 428, ErrorCategory.CONFLICT, false),
    INVALID_STATUS_TRANSITION("TM-WTR-002", 409, ErrorCategory.CONFLICT, false),

    // --- Video consultation ---
    VIDEO_SESSION_NOT_FOUND("TM-VID-001", 404, ErrorCategory.NOT_FOUND, false),
    VIDEO_PROVIDER_UNAVAILABLE("TM-VID-002", 503, ErrorCategory.PROVIDER, true),
    VIDEO_TOKEN_ISSUE_FAILED("TM-VID-003", 502, ErrorCategory.PROVIDER, true),

    // --- Telemetry (ANURA) ---
    TELEMETRY_NOT_ENABLED("TM-TLM-001", 403, ErrorCategory.CONFLICT, false),
    TELEMETRY_MEASUREMENT_FAILED("TM-TLM-002", 502, ErrorCategory.PROVIDER, true),

    // --- Clinical documents ---
    DOCUMENT_NOT_FOUND("TM-DOC-001", 404, ErrorCategory.NOT_FOUND, false),
    DOCUMENT_GENERATION_FAILED("TM-DOC-002", 500, ErrorCategory.INTERNAL, true),
    DOCUMENT_ACCESS_DENIED("TM-DOC-003", 403, ErrorCategory.AUTHORIZATION, false),
    PRESCRIPTION_ISSUE_FAILED("TM-DOC-004", 502, ErrorCategory.PROVIDER, true),

    // --- Survey ---
    SURVEY_NOT_FOUND("TM-SRV-001", 404, ErrorCategory.NOT_FOUND, false),
    SURVEY_ALREADY_SUBMITTED("TM-SRV-002", 409, ErrorCategory.CONFLICT, false),

    // --- Notification ---
    NOTIFICATION_TEMPLATE_NOT_FOUND("TM-NTF-001", 404, ErrorCategory.NOT_FOUND, false),
    NOTIFICATION_CHANNEL_UNAVAILABLE("TM-NTF-002", 503, ErrorCategory.PROVIDER, true),

    // --- Configuration ---
    CONFIG_KEY_NOT_FOUND("TM-CFG-001", 404, ErrorCategory.NOT_FOUND, false),
    CONFIG_VALUE_INVALID("TM-CFG-002", 400, ErrorCategory.VALIDATION, false),

    // --- Audit ---
    AUDIT_WRITE_FAILED("TM-ADT-001", 500, ErrorCategory.INTERNAL, true),
    AUDIT_CHAIN_BROKEN("TM-ADT-002", 500, ErrorCategory.INTERNAL, false),

    // --- Downstream transport ---
    EXTERNAL_TIMEOUT("TM-ITG-002", 504, ErrorCategory.INTEGRATION, true),
    CIRCUIT_OPEN("TM-ITG-003", 503, ErrorCategory.INTEGRATION, true),
    SERVICE_UNAVAILABLE("TM-INT-002", 503, ErrorCategory.INTERNAL, true),
    NOT_IMPLEMENTED("TM-INT-003", 501, ErrorCategory.INTERNAL, false);

    /** Base URI for the {@code type} member of an RFC 9457 problem document. */
    public static final String PROBLEM_TYPE_BASE = "https://telemedicine.bupa/problems/";

    private final String code;
    private final int httpStatus;
    private final ErrorCategory category;
    private final boolean retryable;

    ErrorCode(String code, int httpStatus, ErrorCategory category, boolean retryable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.category = category;
        this.retryable = retryable;
    }

    /** Stable wire code, safe to publish and to match on from clients. */
    public String code() {
        return code;
    }

    /** HTTP status this error maps to when surfaced over REST. */
    public int httpStatus() {
        return httpStatus;
    }

    public ErrorCategory category() {
        return category;
    }

    /**
     * Whether retrying the identical request could plausibly succeed. Callers must still respect
     * idempotency: a retryable error does not make a non-idempotent operation safe to repeat.
     */
    public boolean retryable() {
        return retryable;
    }

    /** Dereferenceable problem type, for example {@code .../problems/consent-required}. */
    public URI problemType() {
        return URI.create(PROBLEM_TYPE_BASE + slug());
    }

    /** Human-readable title used as the RFC 9457 {@code title} member. */
    public String title() {
        String[] words = name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder(name().length());
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Resolves a wire code back to its constant.
     *
     * @throws IllegalArgumentException when the code is not part of the catalogue
     */
    public static ErrorCode fromCode(String wireCode) {
        for (ErrorCode candidate : values()) {
            if (candidate.code.equals(wireCode)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown error code: " + wireCode);
    }
}
