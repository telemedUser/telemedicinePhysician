package com.telemedicine.common.core.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.dto.Violation;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void carriesCodeAndMessage() {
        DomainException exception = new DomainException(ErrorCode.CONFLICT, "already checked in");

        assertThat(exception.getCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("already checked in");
        assertThat(exception.getDetails()).isEmpty();
        assertThat(exception.getCorrelationId()).isNull();
        assertThat(exception.isRetryable()).isFalse();
    }

    @Test
    void requiresAnErrorCode() {
        assertThatThrownBy(() -> new DomainException(null, "boom"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detailsAreDefensivelyCopiedAndImmutable() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("appointmentId", "APT-1");
        DomainException exception = new DomainException(ErrorCode.NOT_FOUND, "gone", source);

        source.put("mutated", true);

        assertThat(exception.getDetails()).containsExactly(Map.entry("appointmentId", "APT-1"));
        assertThatThrownBy(() -> exception.getDetails().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preservesTheCause() {
        Throwable cause = new IllegalStateException("socket closed");
        DomainException exception =
                new DomainException(ErrorCode.INTEGRATION_ERROR, "his call failed", cause);

        assertThat(exception).hasCause(cause);
        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    void withCorrelationIdReturnsACopy() {
        DomainException original = new DomainException(ErrorCode.CONFLICT, "clash");
        DomainException copy = original.withCorrelationId("01JAAAA");

        assertThat(original.getCorrelationId()).isNull();
        assertThat(copy.getCorrelationId()).isEqualTo("01JAAAA");
        assertThat(copy.getCode()).isEqualTo(original.getCode());
        assertThat(copy.getMessage()).isEqualTo(original.getMessage());
    }

    @Test
    void detailsWithCauseOverload() {
        Throwable cause = new IllegalArgumentException("bad");
        DomainException exception =
                new DomainException(ErrorCode.CONFLICT, "clash", Map.of("k", "v"), cause);

        assertThat(exception.getDetails()).containsEntry("k", "v");
        assertThat(exception).hasCause(cause);
    }

    @Test
    void notFoundExposesResourceCoordinates() {
        NotFoundException exception = NotFoundException.of("Appointment", "APT-9");

        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(exception.getDetails())
                .containsEntry("resourceType", "Appointment")
                .containsEntry("resourceId", "APT-9");
        assertThat(new NotFoundException("plain").getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "doc").getCode())
                .isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND);
    }

    @Test
    void validationCarriesFieldViolations() {
        ValidationException exception = ValidationException.field("patient.rut", "must be present");

        assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(exception.getViolations())
                .singleElement()
                .satisfies(
                        violation -> {
                            assertThat(violation.field()).isEqualTo("patient.rut");
                            assertThat(violation.message()).isEqualTo("must be present");
                            assertThat(violation.rejectedValue()).isNull();
                        });
        assertThat(new ValidationException("no violations").getViolations()).isEmpty();
        assertThat(new ValidationException("null list", (List<Violation>) null).getViolations())
                .isEmpty();
        assertThat(new ValidationException("with details", Map.of("k", "v")).getDetails())
                .containsEntry("k", "v");
        assertThat(
                        new ValidationException(ErrorCode.CONFIG_VALUE_INVALID, "bad", List.of())
                                .getCode())
                .isEqualTo(ErrorCode.CONFIG_VALUE_INVALID);
    }

    @Test
    void conflictDescribesRejectedTransitions() {
        ConflictException exception = ConflictException.invalidTransition("OPEN_PATIENT", "CLOSED");

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(exception.getDetails())
                .containsEntry("from", "OPEN_PATIENT")
                .containsEntry("to", "CLOSED");
        assertThat(new ConflictException("plain").getCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(new ConflictException(ErrorCode.SURVEY_ALREADY_SUBMITTED, "dup").getCode())
                .isEqualTo(ErrorCode.SURVEY_ALREADY_SUBMITTED);
        assertThat(new ConflictException(ErrorCode.CONFLICT, "dup", Map.of("id", "1")).getDetails())
                .containsEntry("id", "1");
    }

    @Test
    void forbiddenNamesTheMissingRole() {
        ForbiddenException exception = ForbiddenException.missingRole("ROLE_PHYSICIAN");

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INSUFFICIENT_ROLE);
        assertThat(exception.getDetails()).containsEntry("requiredRole", "ROLE_PHYSICIAN");
        assertThat(new ForbiddenException("nope").getCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(new ForbiddenException(ErrorCode.DOCUMENT_ACCESS_DENIED, "no").getCode())
                .isEqualTo(ErrorCode.DOCUMENT_ACCESS_DENIED);
        assertThat(new ForbiddenException(ErrorCode.FORBIDDEN, "no", Map.of("a", "b")).getDetails())
                .containsEntry("a", "b");
    }

    @Test
    void unauthorizedDistinguishesExpiredFromInvalid() {
        assertThat(UnauthorizedException.expiredToken().getCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
        assertThat(UnauthorizedException.invalidToken().getCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
        assertThat(new UnauthorizedException("anon").getCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(new UnauthorizedException(ErrorCode.INVALID_TOKEN, "x").getCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void integrationAlwaysAttributesTheSystem() {
        IntegrationException timeout = IntegrationException.timeout("fonasa");
        IntegrationException open = IntegrationException.circuitOpen("his");
        IntegrationException plain = new IntegrationException("sap", "rejected");
        IntegrationException wrapped =
                new IntegrationException("sap", "rejected", new IllegalStateException("x"));

        assertThat(timeout.getCode()).isEqualTo(ErrorCode.EXTERNAL_TIMEOUT);
        assertThat(timeout.getSystem()).isEqualTo("fonasa");
        assertThat(timeout.getDetails()).containsEntry("system", "fonasa");
        assertThat(open.getCode()).isEqualTo(ErrorCode.CIRCUIT_OPEN);
        assertThat(plain.getCode()).isEqualTo(ErrorCode.INTEGRATION_ERROR);
        assertThat(wrapped).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void providerOutageNamesTheProviderSoFailoverCanReact() {
        ProviderOutageException exception = new ProviderOutageException("vonage", "no capacity");

        assertThat(exception.getProvider()).isEqualTo("vonage");
        assertThat(exception.getDetails()).containsEntry("provider", "vonage");
        assertThat(exception.isRetryable()).isTrue();
        assertThat(
                        new ProviderOutageException(
                                        ErrorCode.VIDEO_PROVIDER_UNAVAILABLE, "acs", "down", null)
                                .getCode())
                .isEqualTo(ErrorCode.VIDEO_PROVIDER_UNAVAILABLE);
    }

    @Test
    void consentReportsWhichConsentIsMissing() {
        assertThat(new ConsentRequiredException("PLATFORM_TERMS").getConsentType())
                .isEqualTo("PLATFORM_TERMS");
        assertThat(ConsentRequiredException.withdrawn("CLINICAL_INFORMED").getCode())
                .isEqualTo(ErrorCode.CONSENT_WITHDRAWN);
        assertThat(ConsentRequiredException.outdated("PLATFORM_TERMS", "2.0"))
                .satisfies(
                        e -> {
                            assertThat(e.getCode()).isEqualTo(ErrorCode.CONSENT_VERSION_OUTDATED);
                            assertThat(e.getMessage()).contains("2.0");
                        });
    }

    @Test
    void identityFailuresNeverEchoTheSubmittedValue() {
        assertThat(IdentityValidationException.twoFactorRequired().getCode())
                .isEqualTo(ErrorCode.IDENTITY_2FA_REQUIRED);
        assertThat(IdentityValidationException.twoFactorInvalid(2).getDetails())
                .containsEntry("attemptsRemaining", 2);
        assertThat(IdentityValidationException.lockedOut(900L).getDetails())
                .containsEntry("retryAfterSeconds", 900L);
        assertThat(new IdentityValidationException("failed").getCode())
                .isEqualTo(ErrorCode.IDENTITY_VALIDATION_FAILED);
        assertThat(new IdentityValidationException(ErrorCode.MPI_PATIENT_NOT_FOUND, "x").getCode())
                .isEqualTo(ErrorCode.MPI_PATIENT_NOT_FOUND);
        assertThat(
                        new IdentityValidationException(
                                        ErrorCode.IDENTITY_2FA_INVALID, "x", Map.of("a", 1))
                                .getDetails())
                .containsEntry("a", 1);
    }

    @Test
    void rateLimitedExposesRetryAfter() {
        RateLimitedException exception = new RateLimitedException(Duration.ofSeconds(30));

        assertThat(exception.getRetryAfter()).isEqualTo(Duration.ofSeconds(30));
        assertThat(exception.getDetails()).containsEntry("retryAfterSeconds", 30L);
        assertThat(exception.isRetryable()).isTrue();
        assertThat(new RateLimitedException(null).getRetryAfter()).isEqualTo(Duration.ZERO);
    }
}
