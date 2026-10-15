package com.telemedicine.common.web.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.dto.ProblemDetail;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.web.ProblemResponses;
import com.telemedicine.common.web.TelemedicineWebProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Makes unsafe operations replay-safe.
 *
 * <p>A patient on a flaky mobile connection retrying "confirm payment" must not be charged twice,
 * and the front end cannot tell a lost response from a failed request. The client sends the same
 * {@code X-Idempotency-Key} on every attempt; the first attempt executes and its response is
 * stored, and later attempts replay that stored response verbatim.
 *
 * <p>The key is bound to a fingerprint of method, path and body. Reusing one key for a genuinely
 * different request is answered with 409 rather than the previous response, since silently
 * returning an unrelated body would be worse than failing.
 */
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyFilter.class);
    private static final String REPLAYED_HEADER = "X-Idempotent-Replay";

    private final IdempotencyStore store;
    private final TelemedicineWebProperties.Idempotency properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String> methods;

    public IdempotencyKeyFilter(
            IdempotencyStore store,
            TelemedicineWebProperties.Idempotency properties,
            ObjectMapper objectMapper) {
        this.store = store;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.methods =
                properties.getMethods().stream().map(m -> m.toUpperCase(Locale.ROOT)).toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!methods.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return true;
        }
        List<String> patterns = properties.getPathPatterns();
        if (patterns.isEmpty()) {
            return false;
        }
        String path = request.getRequestURI();
        return patterns.stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String key = request.getHeader(Headers.IDEMPOTENCY_KEY);
        if (key == null || key.isBlank()) {
            if (properties.isRequired()) {
                writeProblem(
                        request,
                        response,
                        ErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                        Headers.IDEMPOTENCY_KEY + " is required for this operation");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // The body is buffered so it can be both fingerprinted here and read by the controller.
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String fingerprint = fingerprint(cachedRequest);

        Optional<IdempotencyRecord> existing = store.find(key);
        if (existing.isPresent()) {
            replayOrReject(existing.get(), fingerprint, cachedRequest, response, key);
            return;
        }

        if (!store.tryReserve(key, fingerprint, properties.getTtl())) {
            // Another replica claimed the key between the lookup and the reservation.
            writeProblem(
                    cachedRequest,
                    response,
                    ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                    "A request with this idempotency key is already in progress");
            return;
        }

        executeAndStore(cachedRequest, response, chain, key);
    }

    private void replayOrReject(
            IdempotencyRecord record,
            String fingerprint,
            HttpServletRequest request,
            HttpServletResponse response,
            String key)
            throws IOException {

        if (!record.fingerprint().equals(fingerprint)) {
            writeProblem(
                    request,
                    response,
                    ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                    "This idempotency key was already used for a different request");
            return;
        }
        if (!record.completed()) {
            writeProblem(
                    request,
                    response,
                    ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                    "The original request with this idempotency key is still in progress");
            return;
        }

        log.debug("Replaying stored response for idempotency key {}", key);
        response.setStatus(record.status());
        response.setHeader(REPLAYED_HEADER, "true");
        if (record.contentType() != null) {
            response.setContentType(record.contentType());
        }
        if (record.responseBody() != null) {
            byte[] body = record.responseBody().getBytes(StandardCharsets.UTF_8);
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
        }
        response.flushBuffer();
    }

    private void executeAndStore(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain, String key)
            throws ServletException, IOException {

        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        boolean stored = false;
        try {
            chain.doFilter(request, cachedResponse);

            int status = cachedResponse.getStatus();
            // Only successful outcomes are replayable. Storing a 500 would pin a transient failure
            // to the key and stop the client ever succeeding with it.
            if (status < 400) {
                String body =
                        new String(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                store.complete(
                        key, status, body, cachedResponse.getContentType(), properties.getTtl());
                stored = true;
            }
        } finally {
            cachedResponse.copyBodyToResponse();
            if (!stored) {
                store.release(key);
            }
        }
    }

    private void writeProblem(
            HttpServletRequest request, HttpServletResponse response, ErrorCode code, String detail)
            throws IOException {
        ProblemDetail problem = ProblemResponses.builder(code, request).detail(detail).build();
        response.setStatus(problem.status());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String correlationId = ProblemResponses.correlationId(request);
        if (correlationId != null) {
            response.setHeader(Headers.CORRELATION_ID, correlationId);
        }
        objectMapper.writeValue(response.getOutputStream(), problem);
        response.flushBuffer();
    }

    private static String fingerprint(CachedBodyHttpServletRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(request.getMethod().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '|');
            digest.update(request.getRequestURI().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '|');
            String query = request.getQueryString();
            if (query != null) {
                digest.update(query.getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte) '|');
            digest.update(request.body());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }
}
