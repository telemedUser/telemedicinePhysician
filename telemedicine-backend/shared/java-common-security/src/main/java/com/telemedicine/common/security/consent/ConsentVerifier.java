package com.telemedicine.common.security.consent;

/**
 * Service-provider interface behind {@link RequiresConsent}.
 *
 * <p>Implemented in Phase 4 by the service that owns the consent ledger. Implementations are called
 * on the request thread, so they must be fast and must not throw for an ordinary "no consent"
 * answer — that is a {@link ConsentDecision}, not an exception.
 */
@FunctionalInterface
public interface ConsentVerifier {

    /**
     * @param subjectId patient the consent belongs to; never null or blank
     * @param consentType value from the annotation, for example {@code PLATFORM_TERMS}
     * @param requiredVersion minimum version, or null when any accepted version will do
     */
    ConsentDecision verify(String subjectId, String consentType, String requiredVersion);
}
