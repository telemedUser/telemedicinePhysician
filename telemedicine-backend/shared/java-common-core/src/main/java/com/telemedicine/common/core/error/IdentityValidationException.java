package com.telemedicine.common.core.error;

import java.util.Map;

/**
 * Patient identity verification or FONASA two-factor validation failed.
 *
 * <p>Messages on this type are surfaced to patients, so they must never echo the submitted
 * identifier or the challenge value.
 */
public class IdentityValidationException extends DomainException {

    public IdentityValidationException(String message) {
        super(ErrorCode.IDENTITY_VALIDATION_FAILED, message);
    }

    public IdentityValidationException(ErrorCode code, String message) {
        super(code, message);
    }

    public IdentityValidationException(
            ErrorCode code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    public static IdentityValidationException twoFactorRequired() {
        return new IdentityValidationException(
                ErrorCode.IDENTITY_2FA_REQUIRED, "Two-factor validation is required");
    }

    public static IdentityValidationException twoFactorInvalid(int attemptsRemaining) {
        return new IdentityValidationException(
                ErrorCode.IDENTITY_2FA_INVALID,
                "The verification code is not valid",
                Map.of("attemptsRemaining", attemptsRemaining));
    }

    public static IdentityValidationException lockedOut(long retryAfterSeconds) {
        return new IdentityValidationException(
                ErrorCode.IDENTITY_LOCKED_OUT,
                "Too many failed attempts; identity validation is temporarily locked",
                Map.of("retryAfterSeconds", retryAfterSeconds));
    }
}
