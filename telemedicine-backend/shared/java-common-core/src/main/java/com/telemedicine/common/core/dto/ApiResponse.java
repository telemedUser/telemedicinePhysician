package com.telemedicine.common.core.dto;

import com.telemedicine.common.core.error.ErrorCode;

/**
 * Envelope for successful REST responses.
 *
 * <p><strong>This record's shape is frozen.</strong> The same three members are mirrored in {@code
 * shared/contracts/common-error.yaml}, in the Python {@code ApiResponse} and in the front-end
 * {@code shared-types} package, so adding a component here silently breaks three other languages as
 * well as every compiled consumer. Request-scoped metadata (correlation id, timing, pagination)
 * travels in headers or in {@code PageResponse}, never in this envelope.
 *
 * <p>Failures are represented as RFC 9457 problem documents ({@link ProblemDetail}), not as this
 * type with {@code success=false}; the {@code error} factories exist only for the handful of legacy
 * call sites that still return the envelope on both paths.
 *
 * @param <T> payload type
 */
public record ApiResponse<T>(boolean success, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Success with no payload, for operations whose result is the status code alone. */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /** Success carrying an advisory message, for example a partial-result warning. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    /**
     * Failure carrying the catalogue code. Because the record has no dedicated field, the stable
     * code is prefixed onto the message as {@code "TM-VAL-001: ..."} so it survives on the wire.
     */
    public static <T> ApiResponse<T> error(ErrorCode code, String message) {
        return new ApiResponse<>(false, null, code.code() + ": " + message);
    }
}
