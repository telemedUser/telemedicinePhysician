package com.telemedicine.common.web.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.time.FixedTimeProvider;
import com.telemedicine.common.web.TelemedicineWebProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Covers the branches that are awkward to reach through a full MVC context. */
class IdempotencyKeyFilterUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FixedTimeProvider clock = FixedTimeProvider.atEpoch();
    private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(clock);
    private final TelemedicineWebProperties.Idempotency properties = configuration();

    @Test
    @DisplayName("a missing key is rejected when the endpoint requires one")
    void rejectsMissingKeyWhenRequired() throws Exception {
        properties.setRequired(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request(null, "{}"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString())
                .contains(ErrorCode.IDEMPOTENCY_KEY_REQUIRED.code())
                .contains(Headers.IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("a key whose original request is still running is refused, not queued")
    void rejectsAKeyStillInFlight() throws Exception {
        MockHttpServletRequest request = request("k1", "{\"a\":1}");
        store.tryReserve("k1", fingerprintOf(request), Duration.ofHours(1));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString())
                .contains("still in progress")
                .contains(ErrorCode.IDEMPOTENCY_KEY_CONFLICT.code());
    }

    @Test
    @DisplayName(
            "losing the reservation race is answered with a conflict, not a duplicate execution")
    void rejectsWhenAnotherReplicaWinsTheReservation() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        IdempotencyStore racingStore =
                new InMemoryIdempotencyStore(clock) {
                    @Override
                    public java.util.Optional<IdempotencyRecord> find(String key) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public boolean tryReserve(String key, String fingerprint, Duration ttl) {
                        return false;
                    }
                };

        new IdempotencyKeyFilter(racingStore, properties, objectMapper)
                .doFilter(request("k2", "{}"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("already in progress");
    }

    @Test
    void echoesTheCorrelationIdOnRejections() throws Exception {
        properties.setRequired(true);
        MockHttpServletRequest request = request(null, "{}");
        request.addHeader(Headers.CORRELATION_ID, "01JCORR");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(Headers.CORRELATION_ID)).isEqualTo("01JCORR");
    }

    @Test
    void skipsSafeMethodsAndUnmatchedPaths() {
        IdempotencyKeyFilter filter = filter();

        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/v1/payments")))
                .isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/internal/x")))
                .isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/v1/payments")))
                .isFalse();
    }

    @Test
    @DisplayName("an empty path-pattern list means every unsafe request is covered")
    void emptyPatternListCoversEverything() {
        properties.setPathPatterns(List.of());

        assertThat(filter().shouldNotFilter(new MockHttpServletRequest("POST", "/anything")))
                .isFalse();
    }

    @Test
    void aBlankKeyIsTreatedAsAbsent() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request("   ", "{}"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    private IdempotencyKeyFilter filter() {
        return new IdempotencyKeyFilter(store, properties, objectMapper);
    }

    private static TelemedicineWebProperties.Idempotency configuration() {
        TelemedicineWebProperties.Idempotency configuration =
                new TelemedicineWebProperties().getIdempotency();
        configuration.setEnabled(true);
        configuration.setPathPatterns(List.of("/v1/**"));
        return configuration;
    }

    private static MockHttpServletRequest request(String key, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/payments");
        request.setContentType("application/json");
        request.setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (key != null) {
            request.addHeader(Headers.IDEMPOTENCY_KEY, key);
        }
        return request;
    }

    /** Mirrors the filter's own fingerprint so a pre-seeded record matches. */
    private static String fingerprintOf(MockHttpServletRequest request) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        digest.update(request.getMethod().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        digest.update((byte) '|');
        digest.update(request.getRequestURI().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        digest.update((byte) '|');
        digest.update((byte) '|');
        digest.update(request.getContentAsByteArray());
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
}
