package com.telemedicine.common.security.consent;

import com.telemedicine.common.core.error.ErrorCode;

/**
 * Outcome of a consent check.
 *
 * <p>Distinguishing missing from withdrawn from outdated matters to the caller: each routes the
 * patient to a different screen, and collapsing them into one 403 would make that impossible.
 *
 * @param granted whether the invocation may proceed
 * @param reason catalogue code explaining a refusal; null when granted
 * @param requiredVersion consent version the subject must accept, when that is the obstacle
 */
public record ConsentDecision(boolean granted, ErrorCode reason, String requiredVersion) {

    private static final ConsentDecision GRANTED = new ConsentDecision(true, null, null);

    /** Named as a verb because {@code granted()} is taken by the record's own accessor. */
    public static ConsentDecision grant() {
        return GRANTED;
    }

    /** No consent of this type has been recorded for the subject. */
    public static ConsentDecision missing() {
        return new ConsentDecision(false, ErrorCode.CONSENT_REQUIRED, null);
    }

    /** Consent existed and the subject has since withdrawn it. */
    public static ConsentDecision withdrawn() {
        return new ConsentDecision(false, ErrorCode.CONSENT_WITHDRAWN, null);
    }

    /** Consent exists but predates the version this operation requires. */
    public static ConsentDecision outdated(String requiredVersion) {
        return new ConsentDecision(false, ErrorCode.CONSENT_VERSION_OUTDATED, requiredVersion);
    }
}
