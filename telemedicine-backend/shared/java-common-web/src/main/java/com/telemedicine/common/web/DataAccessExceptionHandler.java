package com.telemedicine.common.web;

import com.telemedicine.common.core.dto.ProblemDetail;
import com.telemedicine.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps Spring's DAO exceptions onto 409 responses.
 *
 * <p>Separate from {@link GlobalExceptionHandler} because {@code org.springframework.dao} ships in
 * spring-tx: web must not force a persistence dependency on services that have none, so this advice
 * is registered conditionally.
 *
 * <p>Both cases are genuinely 409 rather than 500. A version clash means someone else changed the
 * row first, and a unique-constraint breach means the resource already exists — in the appointment
 * and waiting-room flows these are ordinary concurrent outcomes, and returning 500 would make
 * clients give up instead of re-reading and retrying.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 200)
public class DataAccessExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DataAccessExceptionHandler.class);

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(
            OptimisticLockingFailureException exception, HttpServletRequest request) {
        log.warn("Optimistic lock clash on {}", safePath(request));
        return ProblemResponses.render(
                ProblemResponses.builder(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, request)
                        .detail("The resource was modified concurrently; re-read it and retry")
                        .build(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        // The driver message quotes column values, which for this platform means PHI. It is logged
        // for operators but never surfaced in the response.
        log.warn("Data integrity violation on {}", safePath(request), exception);
        return ProblemResponses.render(
                ProblemResponses.builder(ErrorCode.CONFLICT, request)
                        .detail("The request conflicts with existing data")
                        .build(),
                request);
    }

    private static String safePath(HttpServletRequest request) {
        return request == null ? "-" : request.getRequestURI();
    }
}
