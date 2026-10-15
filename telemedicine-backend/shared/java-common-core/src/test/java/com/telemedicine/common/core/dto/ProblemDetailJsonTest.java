package com.telemedicine.common.core.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.error.ErrorCode;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemDetailJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("extension members serialise at the top level, as RFC 9457 requires")
    void extensionsAreFlattened() throws Exception {
        ProblemDetail problem =
                ProblemDetails.of(ErrorCode.CONSENT_REQUIRED)
                        .detail("accept the terms")
                        .instance("/v1/appointments/APT-1")
                        .correlationId("01JCORR")
                        .timestamp(Instant.parse("2025-05-05T12:00:00Z"))
                        .build();

        JsonNode json = objectMapper.valueToTree(problem);

        assertThat(json.has("extensions")).isFalse();
        assertThat(json.get("type").asText())
                .isEqualTo("https://telemedicine.bupa/problems/consent-required");
        assertThat(json.get("status").asInt()).isEqualTo(403);
        assertThat(json.get("code").asText()).isEqualTo("TM-CNS-001");
        assertThat(json.get("correlationId").asText()).isEqualTo("01JCORR");
        assertThat(json.get("category").asText()).isEqualTo("CONSENT");
        assertThat(json.get("timestamp").asText()).isEqualTo("2025-05-05T12:00:00Z");
    }

    @Test
    void omitsAbsentOptionalMembers() throws Exception {
        JsonNode json = objectMapper.valueToTree(ProblemDetails.ofStatus(418, "Teapot").build());

        assertThat(json.has("detail")).isFalse();
        assertThat(json.has("instance")).isFalse();
        assertThat(json.has("code")).isFalse();
    }

    @Test
    @DisplayName("a flattened document parses back into the same object")
    void roundTrips() throws Exception {
        ProblemDetail original =
                ProblemDetails.of(ErrorCode.VALIDATION_ERROR)
                        .detail("bad input")
                        .instance("/v1/appointments")
                        .correlationId("01JCORR")
                        .violation(Violation.of("name", "must not be blank"))
                        .build();

        String json = objectMapper.writeValueAsString(original);
        ProblemDetail parsed = objectMapper.readValue(json, ProblemDetail.class);

        assertThat(parsed.type()).isEqualTo(original.type());
        assertThat(parsed.title()).isEqualTo(original.title());
        assertThat(parsed.status()).isEqualTo(original.status());
        assertThat(parsed.detail()).isEqualTo(original.detail());
        assertThat(parsed.instance()).isEqualTo(original.instance());
        assertThat(parsed.code()).isEqualTo(original.code());
        assertThat(parsed.extensions())
                .containsEntry(ProblemDetails.EXT_CORRELATION_ID, "01JCORR")
                .containsEntry(ProblemDetails.EXT_CATEGORY, "VALIDATION")
                .containsKey(ProblemDetails.EXT_VIOLATIONS);
    }

    @Test
    void toleratesAnEmptyDocument() {
        ProblemDetail parsed = ProblemDetail.fromJson(Map.of());

        assertThat(parsed.type()).isNull();
        assertThat(parsed.status()).isZero();
        assertThat(parsed.extensions()).isEmpty();
    }

    @Test
    void constructorNormalisesNullExtensions() {
        ProblemDetail problem = new ProblemDetail(null, "t", 400, null, null, null, null);

        assertThat(problem.extensions()).isNotNull().isEmpty();
    }
}
