package com.telemedicine.common.core.error;

/** The caller could not be authenticated: absent, malformed or expired credentials. */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode code, String message) {
        super(code, message);
    }

    public static UnauthorizedException expiredToken() {
        return new UnauthorizedException(ErrorCode.TOKEN_EXPIRED, "Access token has expired");
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCode.INVALID_TOKEN, "Access token is not valid");
    }
}
