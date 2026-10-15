package com.telemedicine.common.web;

import com.telemedicine.common.core.dto.ProblemDetail;
import com.telemedicine.common.core.dto.ProblemDetails;
import com.telemedicine.common.core.dto.Violation;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.RateLimitedException;
import com.telemedicine.common.core.error.ValidationException;
import com.telemedicine.common.core.phi.PhiRedactor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates every exception that escapes a controller into an RFC 9457 problem document.
 *
 * <p>Two invariants hold for all branches. Nothing derived from user input reaches the response
 * without passing through {@link PhiRedactor}, because a validation message that quotes the
 * offending value is the most common way PHI ends up in an access log. And unexpected faults never
 * expose the exception type or message unless {@code telemedicine.web.expose-internal-errors} is
 * explicitly switched on for local debugging.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final TelemedicineWebProperties properties;

    public GlobalExceptionHandler(TelemedicineWebProperties properties) {
        this.properties = properties;
    }

    // --- Expected business outcomes -------------------------------------------------

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            ValidationException exception, HttpServletRequest request) {
        ProblemDetails problem =
                ProblemResponses.builder(exception.getCode(), request)
                        .detail(exception.getMessage())
                        .violations(exception.getViolations());
        exception.getDetails().forEach(problem::extension);
        return respond(problem.build(), request, exception);
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ProblemDetail> handleRateLimited(
            RateLimitedException exception, HttpServletRequest request) {
        ResponseEntity<ProblemDetail> response =
                respond(
                        ProblemResponses.builder(exception.getCode(), request)
                                .detail(exception.getMessage())
                                .build(),
                        request,
                        exception);
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header(
                        HttpHeaders.RETRY_AFTER,
                        String.valueOf(exception.getRetryAfter().toSeconds()))
                .body(response.getBody());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(
            DomainException exception, HttpServletRequest request) {
        ProblemDetails problem =
                ProblemResponses.builder(exception.getCode(), request)
                        .detail(exception.getMessage());
        exception.getDetails().forEach(problem::extension);
        return respond(problem.build(), request, exception);
    }

    // --- Malformed requests ---------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Violation> violations = new ArrayList<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            violations.add(
                    new Violation(
                            error.getField(),
                            error.getDefaultMessage(),
                            redactRejectedValue(error.getRejectedValue())));
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            violations.add(Violation.of(error.getObjectName(), error.getDefaultMessage()));
        }
        return respond(
                ProblemResponses.builder(ErrorCode.VALIDATION_ERROR, request)
                        .detail("Request validation failed")
                        .violations(violations)
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<Violation> violations =
                exception.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        new Violation(
                                                String.valueOf(violation.getPropertyPath()),
                                                violation.getMessage(),
                                                redactRejectedValue(violation.getInvalidValue())))
                        .toList();
        return respond(
                ProblemResponses.builder(ErrorCode.VALIDATION_ERROR, request)
                        .detail("Request validation failed")
                        .violations(violations)
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        // The parser message can quote the offending JSON fragment, so it is never echoed.
        return respond(
                ProblemResponses.builder(ErrorCode.MALFORMED_REQUEST, request)
                        .detail("Request body could not be parsed")
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return respond(
                ProblemResponses.builder(ErrorCode.VALIDATION_ERROR, request)
                        .detail("Parameter has the wrong type")
                        .violation(
                                new Violation(
                                        exception.getName(),
                                        "expected " + describeRequiredType(exception),
                                        redactRejectedValue(exception.getValue())))
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return respond(
                ProblemResponses.builder(ErrorCode.MISSING_PARAMETER, request)
                        .detail("A required parameter is missing")
                        .violation(Violation.of(exception.getParameterName(), "is required"))
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return respond(
                ProblemResponses.builder(ErrorCode.METHOD_NOT_ALLOWED, request)
                        .detail(exception.getMethod() + " is not supported by this resource")
                        // Rendered as plain strings: HttpMethod has no Jackson serializer, and
                        // failing to serialise the problem document turns a 405 into a 500.
                        .extension(
                                "supportedMethods",
                                nullSafeStrings(exception.getSupportedMethods()))
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return respond(
                ProblemResponses.builder(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request)
                        .detail("Unsupported content type")
                        .extension(
                                "supportedMediaTypes",
                                exception.getSupportedMediaTypes().stream()
                                        .map(Object::toString)
                                        .toList())
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handlePayloadTooLarge(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return respond(
                ProblemResponses.builder(ErrorCode.PAYLOAD_TOO_LARGE, request)
                        .detail("Upload exceeds the maximum permitted size")
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return respond(
                ProblemResponses.builder(ErrorCode.NOT_FOUND, request)
                        .detail("No handler for this path")
                        .build(),
                request,
                exception);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemResponses.statusBuilder(
                                exception.getStatusCode().value(),
                                exception.getStatusCode().toString(),
                                request)
                        .detail(exception.getReason())
                        .build();
        return respond(problem, request, exception);
    }

    // --- Anything unexpected --------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        ProblemDetails problem =
                ProblemResponses.builder(ErrorCode.INTERNAL_ERROR, request)
                        .detail("An unexpected error occurred");
        if (properties.isExposeInternalErrors()) {
            problem.extension("exception", exception.getClass().getName());
            problem.extension("exceptionMessage", PhiRedactor.redact(exception.getMessage()));
        }
        return respond(problem.build(), request, exception);
    }

    // --- Shared response and logging path -------------------------------------------

    private ResponseEntity<ProblemDetail> respond(
            ProblemDetail problem, HttpServletRequest request, Exception exception) {
        logProblem(problem, request, exception);
        return ProblemResponses.render(problem, request);
    }

    private void logProblem(
            ProblemDetail problem, HttpServletRequest request, Exception exception) {
        String method = request == null ? "-" : request.getMethod();
        String path = problem.instance() == null ? "-" : problem.instance();

        if (problem.status() >= 500) {
            log.error(
                    "{} {} failed with {} ({})",
                    method,
                    path,
                    problem.status(),
                    problem.code(),
                    exception);
        } else if (problem.status() == 429 || problem.status() == 409) {
            log.warn(
                    "{} {} rejected with {} ({}): {}",
                    method,
                    path,
                    problem.status(),
                    problem.code(),
                    problem.detail());
        } else {
            log.debug(
                    "{} {} rejected with {} ({}): {}",
                    method,
                    path,
                    problem.status(),
                    problem.code(),
                    problem.detail());
        }
    }

    private static List<String> nullSafeStrings(String[] values) {
        return values == null ? List.of() : List.of(values);
    }

    private static String describeRequiredType(MethodArgumentTypeMismatchException exception) {
        Class<?> required = exception.getRequiredType();
        return required == null ? "a different type" : required.getSimpleName();
    }

    /** Rejected values originate from the caller, so they are redacted before being echoed. */
    private static String redactRejectedValue(Object value) {
        if (value == null) {
            return null;
        }
        String rendered = String.valueOf(value);
        if (rendered.length() > 64) {
            rendered = rendered.substring(0, 64) + "...";
        }
        return PhiRedactor.redact(rendered);
    }
}
