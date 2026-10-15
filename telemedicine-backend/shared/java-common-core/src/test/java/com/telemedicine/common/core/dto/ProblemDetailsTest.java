package com.telemedicine.common.core.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.ConsentRequiredException;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemDetailsTest {

    @Test
    void seedsEveryMemberFromTheCatalogue() {
        ProblemDetail problem = ProblemDetails.of(ErrorCode.CONSENT_REQUIRED).build();

        assertThat(problem.status()).isEqualTo(403);
        assertThat(problem.title()).isEqualTo("Consent Required");
        assertThat(problem.code()).isEqualTo("TM-CNS-001");
        assertThat(problem.type())
                .hasToString("https://telemedicine.bupa/problems/consent-required");
        assertThat(problem.extensions())
                .containsEntry(ProblemDetails.EXT_CATEGORY, "CONSENT")
                .containsEntry(ProblemDetails.EXT_RETRYABLE, false);
    }

    @Test
    void carriesCorrelationTraceAndTimestampAsExtensions() {
        Instant now = Instant.parse("2025-05-05T12:00:00Z");

        ProblemDetail problem =
                ProblemDetails.of(ErrorCode.NOT_FOUND)
                        .detail("no such appointment")
                        .instance("/v1/appointments/APT-1")
                        .correlationId("01JCORR")
                        .traceId("abc123")
                        .timestamp(now)
                        .build();

        assertThat(problem.detail()).isEqualTo("no such appointment");
        assertThat(problem.instance()).isEqualTo("/v1/appointments/APT-1");
        assertThat(problem.extensions())
                .containsEntry(ProblemDetails.EXT_CORRELATION_ID, "01JCORR")
                .containsEntry(ProblemDetails.EXT_TRACE_ID, "abc123")
                .containsEntry(ProblemDetails.EXT_TIMESTAMP, "2025-05-05T12:00:00Z");
    }

    @Test
    @DisplayName("detail is PHI-redacted on the way in, because problem documents reach browsers")
    void redactsDetailAutomatically() {
        ProblemDetail problem =
                ProblemDetails.of(ErrorCode.VALIDATION_ERROR)
                        .detail("rut 12.345.678-5 is already registered to maria@bupa.cl")
                        .build();

        assertThat(problem.detail())
                .contains("[RUT[REDACTED]]")
                .contains("m***@bupa.cl")
                .doesNotContain("12.345.678")
                .doesNotContain("maria@bupa.cl");
    }

    @Test
    void collectsFieldViolations() {
        ProblemDetail problem =
                ProblemDetails.of(ErrorCode.VALIDATION_ERROR)
                        .violation(Violation.of("patient.rut", "must be present"))
                        .violations(List.of(new Violation("page.size", "must be <= 100", "500")))
                        .violations(null)
                        .violation(null)
                        .build();

        assertThat(problem.extensions()).containsKey(ProblemDetails.EXT_VIOLATIONS);
        Object violations = problem.extensions().get(ProblemDetails.EXT_VIOLATIONS);
        assertThat(violations)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(2);
    }

    @Test
    void omitsTheViolationsMemberWhenThereAreNone() {
        ProblemDetail problem = ProblemDetails.of(ErrorCode.CONFLICT).build();

        assertThat(problem.extensions()).doesNotContainKey(ProblemDetails.EXT_VIOLATIONS);
    }

    @Test
    void buildsFromAThrownDomainException() {
        DomainException exception =
                ConsentRequiredException.withdrawn("CLINICAL_INFORMED").withCorrelationId("01JX");

        ProblemDetail problem = ProblemDetails.from(exception).build();

        assertThat(problem.code()).isEqualTo(ErrorCode.CONSENT_WITHDRAWN.code());
        assertThat(problem.detail()).isEqualTo("Consent has been withdrawn");
        assertThat(problem.extensions())
                .containsEntry(ProblemDetails.EXT_CORRELATION_ID, "01JX")
                .containsEntry("consentType", "CLINICAL_INFORMED");
    }

    @Test
    void supportsStatusesWithNoCatalogueEntry() {
        ProblemDetail problem = ProblemDetails.ofStatus(418, "I am a teapot").build();

        assertThat(problem.status()).isEqualTo(418);
        assertThat(problem.title()).isEqualTo("I am a teapot");
        assertThat(problem.type()).hasToString("about:blank");
        assertThat(problem.code()).isNull();
    }

    @Test
    void allowsExplicitOverridesAndDropsNullExtensions() {
        ProblemDetail problem =
                ProblemDetails.of(ErrorCode.CONFLICT)
                        .type(URI.create("https://example.test/custom"))
                        .title("Custom")
                        .status(422)
                        .code("TM-CUSTOM")
                        .extension("nullValue", null)
                        .extension(null, "ignored")
                        .extension("kept", "yes")
                        .timestamp(null)
                        .build();

        assertThat(problem.type()).hasToString("https://example.test/custom");
        assertThat(problem.title()).isEqualTo("Custom");
        assertThat(problem.status()).isEqualTo(422);
        assertThat(problem.code()).isEqualTo("TM-CUSTOM");
        assertThat(problem.extensions())
                .containsEntry("kept", "yes")
                .doesNotContainKey("nullValue")
                .doesNotContainKey(ProblemDetails.EXT_TIMESTAMP);
    }

    @Test
    void producesAnImmutableDocument() {
        ProblemDetail problem = ProblemDetails.of(ErrorCode.CONFLICT).build();

        assertThatThrownBy(() -> problem.extensions().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void legacyFactoryStillWorks() {
        ProblemDetail problem = ProblemDetail.of(404, "Not Found", "gone", "TM-NFD-001");

        assertThat(problem.status()).isEqualTo(404);
        assertThat(problem.type()).hasToString("about:blank");
        assertThat(problem.extensions()).isEmpty();
    }

    @Test
    void violationFactoryDefaultsRejectedValueToNull() {
        assertThat(Violation.of("f", "m").rejectedValue()).isNull();
    }
}
