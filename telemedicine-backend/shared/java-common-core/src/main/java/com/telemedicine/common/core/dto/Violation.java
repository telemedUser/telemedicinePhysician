package com.telemedicine.common.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single field-level validation failure, rendered under the {@code violations} extension member
 * of an RFC 9457 problem document.
 *
 * @param field dotted path to the offending property, for example {@code patient.rut}
 * @param message human-readable, locale-resolved explanation
 * @param rejectedValue the offending value, already PHI-redacted; null when the value cannot be
 *     shown safely
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Violation(String field, String message, String rejectedValue) {

    public static Violation of(String field, String message) {
        return new Violation(field, message, null);
    }
}
