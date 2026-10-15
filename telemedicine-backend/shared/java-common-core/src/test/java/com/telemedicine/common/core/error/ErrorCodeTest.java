package com.telemedicine.common.core.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ErrorCodeTest {

    @Test
    @DisplayName("every wire code is unique, because clients match on it")
    void wireCodesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(seen.add(code.code()))
                    .as("duplicate wire code %s on %s", code.code(), code.name())
                    .isTrue();
        }
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("every constant maps to a real HTTP status and a category")
    void everyConstantIsWellFormed(ErrorCode code) {
        assertThat(code.httpStatus()).isBetween(400, 599);
        assertThat(code.category()).isNotNull();
        assertThat(code.code()).startsWith("TM-").hasSizeGreaterThan(6);
        assertThat(code.title()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("problem type is a dereferenceable URI under the platform namespace")
    void problemTypeIsNamespaced(ErrorCode code) {
        assertThat(code.problemType().toString())
                .startsWith(ErrorCode.PROBLEM_TYPE_BASE)
                .doesNotContain("_");
    }

    @Test
    void titleIsHumanReadable() {
        assertThat(ErrorCode.IDENTITY_2FA_REQUIRED.title()).isEqualTo("Identity 2fa Required");
        assertThat(ErrorCode.NOT_FOUND.title()).isEqualTo("Not Found");
    }

    @Test
    void problemTypeUsesHyphenatedSlug() {
        assertThat(ErrorCode.CONSENT_REQUIRED.problemType())
                .hasToString("https://telemedicine.bupa/problems/consent-required");
    }

    @Test
    void retryabilityMatchesTheNatureOfTheFailure() {
        assertThat(ErrorCode.EXTERNAL_TIMEOUT.retryable()).isTrue();
        assertThat(ErrorCode.CIRCUIT_OPEN.retryable()).isTrue();
        assertThat(ErrorCode.PROVIDER_OUTAGE.retryable()).isTrue();
        assertThat(ErrorCode.VALIDATION_ERROR.retryable()).isFalse();
        assertThat(ErrorCode.CONSENT_REQUIRED.retryable()).isFalse();
    }

    @Test
    void fromCodeRoundTrips() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(ErrorCode.fromCode(code.code())).isSameAs(code);
        }
    }

    @Test
    void fromCodeRejectsUnknownValues() {
        assertThatThrownBy(() -> ErrorCode.fromCode("TM-NOPE-999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TM-NOPE-999");
    }

    @Test
    @DisplayName("the original ten constants keep their names")
    void legacyConstantsSurvive() {
        assertThat(ErrorCode.valueOf("VALIDATION_ERROR")).isNotNull();
        assertThat(ErrorCode.valueOf("NOT_FOUND")).isNotNull();
        assertThat(ErrorCode.valueOf("UNAUTHORIZED")).isNotNull();
        assertThat(ErrorCode.valueOf("FORBIDDEN")).isNotNull();
        assertThat(ErrorCode.valueOf("CONFLICT")).isNotNull();
        assertThat(ErrorCode.valueOf("INTEGRATION_ERROR")).isNotNull();
        assertThat(ErrorCode.valueOf("PROVIDER_OUTAGE")).isNotNull();
        assertThat(ErrorCode.valueOf("CONSENT_REQUIRED")).isNotNull();
        assertThat(ErrorCode.valueOf("IDENTITY_VALIDATION_FAILED")).isNotNull();
        assertThat(ErrorCode.valueOf("INTERNAL_ERROR")).isNotNull();
    }
}
