package com.telemedicine.common.security.consent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissiveConsentVerifierTest {

    @Test
    @DisplayName(
            "the placeholder verifier grants everything, which is why it warns on construction")
    void grantsEveryCheck() {
        PermissiveConsentVerifier verifier = new PermissiveConsentVerifier();

        assertThat(verifier.verify("PAT-1", "PLATFORM_TERMS", null).granted()).isTrue();
        assertThat(verifier.verify("PAT-1", "CLINICAL_INFORMED", "3.0").granted()).isTrue();
    }

    @Test
    void decisionFactoriesCarryTheRightReason() {
        assertThat(ConsentDecision.grant().reason()).isNull();
        assertThat(ConsentDecision.missing().granted()).isFalse();
        assertThat(ConsentDecision.withdrawn().requiredVersion()).isNull();
        assertThat(ConsentDecision.outdated("4.1").requiredVersion()).isEqualTo("4.1");
    }
}
