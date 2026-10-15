package com.telemedicine.common.security.problem;

import com.telemedicine.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Answers an authenticated-but-forbidden request with a problem document.
 *
 * <p>The denial is logged with the requested path and the caller's authorities left out: a 403 on a
 * clinical resource is itself a security event, and the URI may contain a patient identifier, so
 * the log line stays deliberately thin.
 */
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemAccessDeniedHandler.class);

    private final ProblemHttpWriter writer;

    public ProblemAccessDeniedHandler(ProblemHttpWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception)
            throws IOException {
        log.warn(
                "Denied {} {}: insufficient authority",
                request.getMethod(),
                request.getRequestURI());
        writer.write(
                request,
                response,
                ErrorCode.INSUFFICIENT_ROLE,
                "Your account does not have permission to perform this action");
    }
}
