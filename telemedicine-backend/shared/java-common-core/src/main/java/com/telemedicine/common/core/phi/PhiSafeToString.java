package com.telemedicine.common.core.phi;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;

/**
 * Builds a {@code toString()} that masks every member annotated {@link Phi}.
 *
 * <p>Entities and DTOs end up in logs through exception messages, actuator dumps and debug
 * statements that nobody audits. Routing their {@code toString()} through here means a PHI leak
 * requires actively removing the annotation rather than merely forgetting to add masking.
 */
public final class PhiSafeToString {

    private static final int MAX_DEPTH_FIELDS = 60;

    private PhiSafeToString() {}

    /** Reflectively renders {@code target}, masking annotated fields by their declared kind. */
    public static String of(Object target) {
        if (target == null) {
            return "null";
        }
        Class<?> type = target.getClass();
        StringJoiner joiner = new StringJoiner(", ", type.getSimpleName() + "{", "}");
        int rendered = 0;

        for (Class<?> current = type; current != null && current != Object.class; ) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (rendered++ >= MAX_DEPTH_FIELDS) {
                    joiner.add("...");
                    return joiner.toString();
                }
                joiner.add(field.getName() + "=" + renderField(target, field));
            }
            current = current.getSuperclass();
        }
        return joiner.toString();
    }

    private static String renderField(Object target, Field field) {
        Object value;
        try {
            field.setAccessible(true);
            value = field.get(target);
        } catch (IllegalAccessException | RuntimeException e) {
            // Covers SecurityException and InaccessibleObjectException from a closed module.
            // A field we cannot read is a field we cannot leak.
            return "<unreadable>";
        }
        if (value == null) {
            return "null";
        }
        Phi phi = field.getAnnotation(Phi.class);
        if (phi != null) {
            return PhiRedactor.mask(String.valueOf(value), phi.value());
        }
        return PhiRedactor.redact(String.valueOf(value));
    }

    /** Alias kept for readability at call sites: {@code return PhiSafeToString.render(this);} */
    public static String render(Object target) {
        return of(target);
    }
}
