package com.telemedicine.common.core.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IntegrationException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void successCarriesItsValue() {
        Result<String, ErrorCode> result = Result.success("SESSION-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.value()).contains("SESSION-1");
        assertThat(result.error()).isEmpty();
    }

    @Test
    void failureCarriesItsError() {
        Result<String, ErrorCode> result = Result.failure(ErrorCode.PROVIDER_OUTAGE);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).contains(ErrorCode.PROVIDER_OUTAGE);
        assertThat(result.value()).isEmpty();
    }

    @Test
    void mapTransformsOnlyTheSuccessSide() {
        assertThat(Result.<String, ErrorCode>success("abc").map(String::length).value())
                .contains(3);
        assertThat(
                        Result.<String, ErrorCode>failure(ErrorCode.CIRCUIT_OPEN)
                                .map(String::length)
                                .error())
                .contains(ErrorCode.CIRCUIT_OPEN);
    }

    @Test
    void flatMapChainsResults() {
        Result<Integer, ErrorCode> chained =
                Result.<String, ErrorCode>success("42")
                        .flatMap(v -> Result.success(Integer.parseInt(v)));

        assertThat(chained.value()).contains(42);
        assertThat(
                        Result.<String, ErrorCode>failure(ErrorCode.EXTERNAL_TIMEOUT)
                                .flatMap(v -> Result.<Integer, ErrorCode>success(1))
                                .error())
                .contains(ErrorCode.EXTERNAL_TIMEOUT);
    }

    @Test
    void mapErrorTransformsOnlyTheFailureSide() {
        assertThat(
                        Result.<String, ErrorCode>failure(ErrorCode.CIRCUIT_OPEN)
                                .mapError(ErrorCode::code)
                                .error())
                .contains("TM-ITG-003");
        assertThat(Result.<String, ErrorCode>success("ok").mapError(ErrorCode::code).value())
                .contains("ok");
    }

    @Test
    void fallbacksApplyOnlyToFailures() {
        assertThat(Result.<String, ErrorCode>success("live").orElse("fallback")).isEqualTo("live");
        assertThat(Result.<String, ErrorCode>failure(ErrorCode.CIRCUIT_OPEN).orElse("fallback"))
                .isEqualTo("fallback");
        assertThat(Result.<String, ErrorCode>success("live").orElseGet(() -> "lazy"))
                .isEqualTo("live");
        assertThat(
                        Result.<String, ErrorCode>failure(ErrorCode.CIRCUIT_OPEN)
                                .orElseGet(() -> "lazy"))
                .isEqualTo("lazy");
    }

    @Test
    void orElseThrowConvertsAFailureIntoTheDomainException() {
        Result<String, ErrorCode> failure = Result.failure(ErrorCode.PROVIDER_OUTAGE);

        assertThatThrownBy(
                        () ->
                                failure.orElseThrow(
                                        code -> new IntegrationException("vonage", code.name())))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("PROVIDER_OUTAGE");

        assertThat(
                        Result.<String, ErrorCode>success("v")
                                .orElseThrow(code -> new IllegalStateException()))
                .isEqualTo("v");
    }

    @Test
    void bareOrElseThrowReportsTheError() {
        assertThat(Result.<String, ErrorCode>success("v").orElseThrow()).isEqualTo("v");
        assertThatThrownBy(
                        () -> Result.<String, ErrorCode>failure(ErrorCode.CONFLICT).orElseThrow())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("CONFLICT");
    }

    @Test
    void nullPayloadsAreTolerated() {
        assertThat(Result.<String, ErrorCode>success(null).value()).isEmpty();
        assertThat(Result.<String, ErrorCode>failure(null).error()).isEmpty();
    }
}
