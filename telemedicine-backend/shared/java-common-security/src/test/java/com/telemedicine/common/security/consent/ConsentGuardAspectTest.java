package com.telemedicine.common.security.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.telemedicine.common.core.error.ConsentRequiredException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ConsentGuardAspectTest {

    private final RecordingVerifier verifier = new RecordingVerifier();
    private final SecurityContextAccessor securityContext =
            new SecurityContextAccessor(new TelemedicineSecurityProperties().getJwt());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void proceedsWhenConsentIsGranted() {
        authenticateAsPatient("PAT-1");
        verifier.next = ConsentDecision.grant();

        assertThat(proxy().book("APT-1")).isEqualTo("booked APT-1");
        assertThat(verifier.calls).containsExactly("PAT-1|PLATFORM_TERMS|null");
    }

    @Test
    @DisplayName("a missing consent surfaces as the catalogue code the client can route on")
    void refusesWhenConsentIsMissing() {
        authenticateAsPatient("PAT-1");
        verifier.next = ConsentDecision.missing();

        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> proxy().book("APT-1"))
                .satisfies(
                        exception -> {
                            assertThat(exception.getCode()).isEqualTo(ErrorCode.CONSENT_REQUIRED);
                            assertThat(exception.getConsentType()).isEqualTo("PLATFORM_TERMS");
                        });
    }

    @Test
    void distinguishesWithdrawnFromMissing() {
        authenticateAsPatient("PAT-1");
        verifier.next = ConsentDecision.withdrawn();

        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> proxy().book("APT-1"))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CONSENT_WITHDRAWN));
    }

    @Test
    void reportsTheVersionTheSubjectStillHasToAccept() {
        authenticateAsPatient("PAT-1");
        verifier.next = ConsentDecision.outdated("2.1");

        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> proxy().startConsultation("APT-1"))
                .satisfies(
                        exception -> {
                            assertThat(exception.getCode())
                                    .isEqualTo(ErrorCode.CONSENT_VERSION_OUTDATED);
                            assertThat(exception.getMessage()).contains("2.1");
                        });
        assertThat(verifier.calls).containsExactly("APT-1|CLINICAL_INFORMED|2.0");
    }

    @Test
    @DisplayName("the subject can be pulled out of the arguments instead of the security context")
    void resolvesTheSubjectFromASpelExpression() {
        verifier.next = ConsentDecision.grant();

        assertThat(proxy().startConsultation("APT-42")).isEqualTo("started APT-42");
        assertThat(verifier.calls).containsExactly("APT-42|CLINICAL_INFORMED|2.0");
    }

    @Test
    @DisplayName("an unresolvable subject is refused: an unverifiable check is not a passed check")
    void refusesWhenNoSubjectCanBeResolved() {
        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> proxy().book("APT-1"))
                .withMessageContaining("no subject was resolved");
        assertThat(verifier.calls).isEmpty();
    }

    @Test
    void refusesWhenTheVerifierReturnsNothing() {
        authenticateAsPatient("PAT-1");
        verifier.next = null;

        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> proxy().book("APT-1"))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CONSENT_REQUIRED));
    }

    @Test
    void refusesOnADenialThatNamesNoReason() {
        authenticateAsPatient("PAT-1");
        verifier.next = new ConsentDecision(false, null, null);

        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> proxy().book("APT-1"))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CONSENT_REQUIRED));
    }

    @Test
    void fallsBackToTheAnnotatedVersionWhenTheDecisionNamesNone() {
        verifier.next = ConsentDecision.outdated(null);

        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> proxy().startConsultation("APT-1"))
                .withMessageContaining("2.0");
    }

    @Test
    void leavesUnannotatedMethodsAlone() {
        assertThat(proxy().ping()).isEqualTo("pong");
        assertThat(verifier.calls).isEmpty();
    }

    @Test
    @DisplayName("an annotation on the class covers every method it declares")
    void appliesATypeLevelAnnotation() {
        verifier.next = ConsentDecision.missing();
        AspectJProxyFactory factory = new AspectJProxyFactory(new GatedService());
        factory.addAspect(new ConsentGuardAspect(verifier, securityContext));
        GatedService gated = factory.getProxy();

        assertThatExceptionOfType(ConsentRequiredException.class)
                .isThrownBy(() -> gated.anything("SUBJ-1"));
    }

    private ConsentedService proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new ConsentedService());
        factory.addAspect(new ConsentGuardAspect(verifier, securityContext));
        return factory.getProxy();
    }

    private static void authenticateAsPatient(String patientId) {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "RS256")
                        .subject(patientId)
                        .claim("patient_id", patientId)
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                jwt, List.of(new SimpleGrantedAuthority(Roles.PATIENT))));
    }

    static class ConsentedService {

        @RequiresConsent("PLATFORM_TERMS")
        String book(String appointmentId) {
            return "booked " + appointmentId;
        }

        @RequiresConsent(value = "CLINICAL_INFORMED", version = "2.0", subject = "#appointmentId")
        String startConsultation(String appointmentId) {
            return "started " + appointmentId;
        }

        String ping() {
            return "pong";
        }
    }

    @RequiresConsent(value = "PLATFORM_TERMS", subject = "#subjectId")
    static class GatedService {

        String anything(String subjectId) {
            return subjectId;
        }
    }

    static class RecordingVerifier implements ConsentVerifier {

        final List<String> calls = new ArrayList<>();
        ConsentDecision next = ConsentDecision.grant();

        @Override
        public ConsentDecision verify(
                String subjectId, String consentType, String requiredVersion) {
            calls.add(subjectId + "|" + consentType + "|" + requiredVersion);
            return next;
        }
    }
}
