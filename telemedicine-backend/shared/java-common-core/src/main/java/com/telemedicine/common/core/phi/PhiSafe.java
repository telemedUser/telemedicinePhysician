package com.telemedicine.common.core.phi;

/**
 * Original PHI helper, kept for source compatibility with the services that already call it.
 *
 * @deprecated Use {@link PhiRedactor}, which understands Chilean RUTs, e-mail, phone numbers,
 *     tokens and card numbers instead of masking every value the same way.
 */
@Deprecated(since = "1.0.0")
public final class PhiSafe {

    private PhiSafe() {}

    /** Delegates to {@link PhiRedactor#maskGeneric(String)}; behaviour is unchanged. */
    public static String mask(String value) {
        return PhiRedactor.maskGeneric(value);
    }
}
