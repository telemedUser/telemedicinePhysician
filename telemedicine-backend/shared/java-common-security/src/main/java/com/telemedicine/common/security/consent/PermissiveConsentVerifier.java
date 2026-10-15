package com.telemedicine.common.security.consent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default verifier used until the consent ledger exists: grants every check.
 *
 * <p>This is a deliberate fail-open. Failing closed would deny every annotated endpoint in every
 * service that has not yet been wired to Phase 4, turning the introduction of the annotation into
 * an outage. The trade is made visible by a startup warning, and any real {@link ConsentVerifier}
 * bean replaces this one.
 */
public class PermissiveConsentVerifier implements ConsentVerifier {

    private static final Logger log = LoggerFactory.getLogger(PermissiveConsentVerifier.class);

    public PermissiveConsentVerifier() {
        log.warn(
                "No ConsentVerifier bean is defined, so @RequiresConsent grants every request. This"
                    + " is expected before the consent ledger ships; it must not be the case in an"
                    + " environment handling real patients.");
    }

    @Override
    public ConsentDecision verify(String subjectId, String consentType, String requiredVersion) {
        log.debug(
                "Consent check for type {} bypassed: no ConsentVerifier implementation",
                consentType);
        return ConsentDecision.grant();
    }
}
