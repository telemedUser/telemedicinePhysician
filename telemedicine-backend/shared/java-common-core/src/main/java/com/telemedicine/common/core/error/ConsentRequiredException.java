package com.telemedicine.common.core.error;

import java.util.Map;

/**
 * The patient journey cannot proceed because consent is missing, withdrawn or out of date.
 *
 * <p>The design document keeps platform terms and clinical informed consent separate, so the
 * consent type is always reported to let the client route the patient to the right flow.
 */
public class ConsentRequiredException extends DomainException {

    private final String consentType;

    public ConsentRequiredException(String consentType) {
        this(ErrorCode.CONSENT_REQUIRED, consentType, "Consent is required before continuing");
    }

    public ConsentRequiredException(ErrorCode code, String consentType, String message) {
        super(code, message, Map.of("consentType", consentType));
        this.consentType = consentType;
    }

    /** For example {@code PLATFORM_TERMS} or {@code CLINICAL_INFORMED}. */
    public String getConsentType() {
        return consentType;
    }

    public static ConsentRequiredException withdrawn(String consentType) {
        return new ConsentRequiredException(
                ErrorCode.CONSENT_WITHDRAWN, consentType, "Consent has been withdrawn");
    }

    public static ConsentRequiredException outdated(String consentType, String requiredVersion) {
        return new ConsentRequiredException(
                ErrorCode.CONSENT_VERSION_OUTDATED,
                consentType,
                "A newer consent version (" + requiredVersion + ") must be accepted");
    }
}
