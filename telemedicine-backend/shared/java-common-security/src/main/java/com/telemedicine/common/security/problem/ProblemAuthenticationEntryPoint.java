package com.telemedicine.common.security.problem;

import com.telemedicine.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Answers an unauthenticated request with a problem document instead of an empty 401. */
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log =
            LoggerFactory.getLogger(ProblemAuthenticationEntryPoint.class);

    private final ProblemHttpWriter writer;
    private final String realm;

    public ProblemAuthenticationEntryPoint(ProblemHttpWriter writer, String realm) {
        this.writer = writer;
        this.realm = realm == null || realm.isBlank() ? "telemedicine" : realm;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        ErrorCode code = classify(exception);
        // RFC 6750 requires the challenge header; clients use it to decide whether to refresh.
        response.setHeader(
                HttpHeaders.WWW_AUTHENTICATE,
                "Bearer realm=\"" + realm + "\", error=\"" + oauthError(code) + "\"");
        log.warn(
                "Rejected unauthenticated request to {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                code.code());
        // The exception message can quote the token, so only the classification is rendered.
        writer.write(request, response, code, detailFor(code));
    }

    private static ErrorCode classify(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String description = oauthException.getError().getDescription();
            if (description != null && description.toLowerCase(Locale.ROOT).contains("expired")) {
                return ErrorCode.TOKEN_EXPIRED;
            }
            return ErrorCode.INVALID_TOKEN;
        }
        return ErrorCode.UNAUTHORIZED;
    }

    private static String oauthError(ErrorCode code) {
        return code == ErrorCode.UNAUTHORIZED ? "unauthorized" : "invalid_token";
    }

    private static String detailFor(ErrorCode code) {
        return switch (code) {
            case TOKEN_EXPIRED -> "The access token has expired";
            case INVALID_TOKEN -> "The access token is not valid for this API";
            default -> "Authentication is required to access this resource";
        };
    }
}
