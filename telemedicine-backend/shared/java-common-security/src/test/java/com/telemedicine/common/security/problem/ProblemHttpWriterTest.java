package com.telemedicine.common.security.problem;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.error.ErrorCode;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class ProblemHttpWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProblemHttpWriter writer = new ProblemHttpWriter(objectMapper);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("the body is a problem document, not Spring Security's empty 401")
    void writesAProblemDocument() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request(), response, ErrorCode.UNAUTHORIZED, "Authentication is required");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"" + ErrorCode.UNAUTHORIZED.code() + "\"")
                .contains("\"instance\":\"/v1/appointments\"")
                .contains("\"timestamp\"");
    }

    @Test
    @DisplayName("the correlation header is echoed, because runbooks tell support to trace by it")
    void echoesTheCorrelationId() throws IOException {
        MockHttpServletRequest request = request();
        request.addHeader(Headers.CORRELATION_ID, "01JHEADER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request, response, ErrorCode.FORBIDDEN, "no");

        assertThat(response.getHeader(Headers.CORRELATION_ID)).isEqualTo("01JHEADER");
        assertThat(response.getContentAsString()).contains("01JHEADER");
    }

    @Test
    void prefersTheMdcCorrelationIdAndIncludesTheTraceId() throws IOException {
        MDC.put(MdcKeys.CORRELATION_ID, "01JMDC");
        MDC.put(MdcKeys.TRACE_ID, "abc123");
        MockHttpServletRequest request = request();
        request.addHeader(Headers.CORRELATION_ID, "01JHEADER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request, response, ErrorCode.FORBIDDEN, "no");

        assertThat(response.getHeader(Headers.CORRELATION_ID)).isEqualTo("01JMDC");
        assertThat(response.getContentAsString()).contains("abc123");
    }

    @Test
    void toleratesAMissingRequestAndObjectMapper() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ProblemHttpWriter(null).write(null, response, ErrorCode.INTERNAL_ERROR, "boom");

        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test
    @DisplayName("a committed response is left alone rather than corrupted mid-stream")
    void doesNothingOnceTheResponseIsCommitted() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getWriter().write("already sent");
        response.flushBuffer();

        writer.write(request(), response, ErrorCode.UNAUTHORIZED, "too late");

        assertThat(response.getContentAsString()).isEqualTo("already sent");
    }

    @Test
    void entryPointClassifiesAnExpiredToken() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint()
                .commence(
                        request(),
                        response,
                        new InvalidBearerTokenException("Jwt expired at 2026-01-01T00:00:00Z"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains(ErrorCode.TOKEN_EXPIRED.code())
                .doesNotContain("2026-01-01");
        assertThat(response.getHeader("WWW-Authenticate"))
                .isEqualTo("Bearer realm=\"waiting-room-service\", error=\"invalid_token\"");
    }

    @Test
    void entryPointClassifiesAMalformedToken() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint()
                .commence(
                        request(),
                        response,
                        new OAuth2AuthenticationException(
                                new OAuth2Error("invalid_token", "Malformed payload", null)));

        assertThat(response.getContentAsString()).contains(ErrorCode.INVALID_TOKEN.code());
    }

    @Test
    void entryPointFallsBackToPlainUnauthorized() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint()
                .commence(
                        request(),
                        response,
                        new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));

        assertThat(response.getContentAsString()).contains(ErrorCode.INVALID_TOKEN.code());
    }

    @Test
    void entryPointUsesADefaultRealmWhenNoneIsConfigured() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ProblemAuthenticationEntryPoint(writer, " ")
                .commence(
                        request(),
                        response,
                        new org.springframework.security.authentication
                                .InsufficientAuthenticationException("none"));

        assertThat(response.getHeader("WWW-Authenticate"))
                .isEqualTo("Bearer realm=\"telemedicine\", error=\"unauthorized\"");
        assertThat(response.getContentAsString()).contains(ErrorCode.UNAUTHORIZED.code());
    }

    @Test
    @DisplayName("a denial reports insufficient role rather than a bare 403")
    void accessDeniedHandlerWritesAProblemDocument() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ProblemAccessDeniedHandler(writer)
                .handle(request(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains(ErrorCode.INSUFFICIENT_ROLE.code());
    }

    private ProblemAuthenticationEntryPoint entryPoint() {
        return new ProblemAuthenticationEntryPoint(writer, "waiting-room-service");
    }

    private static MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/v1/appointments");
    }
}
