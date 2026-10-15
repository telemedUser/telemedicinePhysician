package com.telemedicine.common.core.phi;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes protected health information from strings before they reach a log, a problem document or
 * an audit record.
 *
 * <p>The patterns are deliberately narrow. Over-redaction is not a safe default here: clinical free
 * text is full of numbers (dosages, blood pressures, ICD codes, durations) and a greedy digit rule
 * would shred exactly the diagnostic detail an on-call engineer needs. Every rule therefore either
 * matches a structurally distinctive format or, for card numbers, additionally validates a Luhn
 * checksum before redacting.
 */
public final class PhiRedactor {

    /** Chilean RUT, with or without thousands separators, for example {@code 12.345.678-5}. */
    private static final Pattern RUT =
            Pattern.compile("(?<![\\w.-])(\\d{1,2}\\.?\\d{3}\\.?\\d{3})-([\\dkK])(?![\\w-])");

    private static final Pattern EMAIL =
            Pattern.compile(
                    "(?<![\\w.])([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})(?![\\w.])");

    /** E.164 numbers only. Bare local digit strings are too ambiguous to match safely. */
    private static final Pattern PHONE = Pattern.compile("(?<![\\d+])(\\+\\d{8,15})(?!\\d)");

    private static final Pattern JWT =
            Pattern.compile("(?<![\\w.])eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]*");

    /** Candidate card numbers; confirmed with a Luhn check before anything is replaced. */
    private static final Pattern CARD_CANDIDATE =
            Pattern.compile("(?<![\\d-])(?:\\d[ -]?){12,18}\\d(?![\\d-])");

    private static final String REDACTED = "[REDACTED]";

    private PhiRedactor() {}

    /**
     * Redacts every recognised identifier in free text. Safe to call on null and on text that
     * contains no PHI, in which case the input is returned unchanged.
     */
    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        result = replaceAll(result, JWT, matcher -> "[JWT" + REDACTED + "]");
        result = replaceAll(result, RUT, matcher -> "[RUT" + REDACTED + "]");
        result = replaceAll(result, EMAIL, PhiRedactor::maskEmailMatch);
        result = replaceAll(result, PHONE, matcher -> maskPhone(matcher.group(1)));
        result = redactCardNumbers(result);
        return result;
    }

    /** Applies the masking rule for a specific classification. */
    public static String mask(String value, PhiKind kind) {
        if (value == null) {
            return null;
        }
        return switch (kind) {
            case RUT, ADDRESS, MRN, PAN, SECRET, CLINICAL_NOTE -> REDACTED;
            case EMAIL -> maskEmail(value);
            case PHONE -> maskPhone(value);
            case NAME -> maskName(value);
            case GENERIC -> maskGeneric(value);
        };
    }

    /** Retains the first two and last two characters, for example {@code ab****yz}. */
    public static String maskGeneric(String value) {
        if (value == null || value.length() < 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /** Keeps the first character of the local part and the whole domain: {@code j***@bupa.cl}. */
    public static String maskEmail(String value) {
        if (value == null) {
            return null;
        }
        int at = value.indexOf('@');
        if (at <= 0 || at == value.length() - 1) {
            return maskGeneric(value);
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    /** Keeps the country code and last two digits: {@code +56*******89}. */
    public static String maskPhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^\\d+]", "");
        if (digits.length() < 6) {
            return "****";
        }
        String prefix =
                digits.startsWith("+") ? digits.substring(0, Math.min(3, digits.length())) : "";
        String suffix = digits.substring(digits.length() - 2);
        int hidden = Math.max(1, digits.length() - prefix.length() - suffix.length());
        return prefix + "*".repeat(hidden) + suffix;
    }

    /** Reduces a name to its initials: {@code María Fernanda Soto} becomes {@code M. F. S.}. */
    public static String maskName(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }
        StringBuilder initials = new StringBuilder();
        for (String part : value.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                if (initials.length() > 0) {
                    initials.append(' ');
                }
                initials.append(Character.toUpperCase(part.charAt(0))).append('.');
            }
        }
        return initials.toString();
    }

    /** Fully redacts a RUT; the check digit alone materially narrows a search. */
    public static String maskRut(String value) {
        return value == null ? null : REDACTED;
    }

    private static String maskEmailMatch(Matcher matcher) {
        return matcher.group(1).charAt(0) + "***@" + matcher.group(2);
    }

    private static String replaceAll(String input, Pattern pattern, Replacer replacer) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length());
        matcher.reset();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacer.replace(matcher)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String redactCardNumbers(String input) {
        Matcher matcher = CARD_CANDIDATE.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length());
        matcher.reset();
        while (matcher.find()) {
            String candidate = matcher.group();
            String replacement = isLuhnValid(candidate) ? "[PAN" + REDACTED + "]" : candidate;
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Luhn checksum. Without it, any 13-to-19 digit run — order references, ANURA measurement ids,
     * epoch-millisecond timestamps — would be mistaken for a card number.
     */
    static boolean isLuhnValid(String candidate) {
        String digits = candidate.replaceAll("[^\\d]", "");
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean doubling = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubling) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubling = !doubling;
        }
        return sum % 10 == 0;
    }

    /** True when the text contains anything the redactor would rewrite. */
    public static boolean containsPhi(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return !redact(text).equals(text);
    }

    /** Resolves a {@link PhiKind} from a field name, used for best-effort structured redaction. */
    public static PhiKind kindForFieldName(String fieldName) {
        if (fieldName == null) {
            return PhiKind.GENERIC;
        }
        String name = fieldName.toLowerCase(Locale.ROOT);
        if (name.contains("rut") || name.contains("nationalid")) {
            return PhiKind.RUT;
        }
        if (name.contains("email") || name.contains("mail")) {
            return PhiKind.EMAIL;
        }
        if (name.contains("phone") || name.contains("mobile") || name.contains("msisdn")) {
            return PhiKind.PHONE;
        }
        if (name.contains("name")) {
            return PhiKind.NAME;
        }
        if (name.contains("address")) {
            return PhiKind.ADDRESS;
        }
        if (name.contains("token") || name.contains("secret") || name.contains("password")) {
            return PhiKind.SECRET;
        }
        return PhiKind.GENERIC;
    }

    @FunctionalInterface
    private interface Replacer {
        String replace(Matcher matcher);
    }
}
