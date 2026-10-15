package com.telemedicine.common.observability.mdc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

/** Snapshot of the current MDC map for propagation across thread hops. */
public final class MdcContext {

    private final Map<String, String> values;

    private MdcContext(Map<String, String> values) {
        this.values = values;
    }

    public static MdcContext capture() {
        Map<String, String> copy = MDC.getCopyOfContextMap();
        if (copy == null || copy.isEmpty()) {
            return new MdcContext(Map.of());
        }
        return new MdcContext(Collections.unmodifiableMap(new HashMap<>(copy)));
    }

    public static MdcContext of(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return new MdcContext(Map.of());
        }
        return new MdcContext(Collections.unmodifiableMap(new HashMap<>(values)));
    }

    public Map<String, String> values() {
        return values;
    }

    public void apply() {
        MDC.clear();
        values.forEach(MDC::put);
    }

    public void clear() {
        values.keySet().forEach(MDC::remove);
    }

    public AutoCloseable scope() {
        MdcContext previous = capture();
        apply();
        return () -> {
            MDC.clear();
            previous.apply();
        };
    }
}
