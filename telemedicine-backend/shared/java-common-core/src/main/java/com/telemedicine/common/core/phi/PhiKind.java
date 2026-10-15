package com.telemedicine.common.core.phi;

/**
 * Classes of protected data, each with its own masking rule.
 *
 * <p>Strong direct identifiers are removed outright. Contact details keep a short, non-identifying
 * fragment because L2 support has to be able to confirm "yes, that is the address we sent it to"
 * while reading a redacted log.
 */
public enum PhiKind {
    /** Chilean national identifier. Fully redacted: the check digit alone narrows the search. */
    RUT,
    /** Email address. Masked to first character plus domain. */
    EMAIL,
    /** Telephone number. Masked to country code plus last two digits. */
    PHONE,
    /** Patient or physician name. Masked to initials. */
    NAME,
    /** Postal address. Fully redacted. */
    ADDRESS,
    /** Medical record number. Fully redacted. */
    MRN,
    /** Primary account number for a card. Fully redacted. */
    PAN,
    /** Bearer token or credential. Fully redacted. */
    SECRET,
    /** Free-text clinical note. Fully redacted; never safe to sample. */
    CLINICAL_NOTE,
    /** Anything else sensitive: first two and last two characters retained. */
    GENERIC
}
