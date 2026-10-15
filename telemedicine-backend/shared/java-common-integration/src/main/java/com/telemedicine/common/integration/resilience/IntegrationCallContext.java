package com.telemedicine.common.integration.resilience;

import com.telemedicine.common.core.constants.Headers;
import org.springframework.http.HttpMethod;

/** Thread-local metadata used by the idempotency-aware retry predicate. */
public final class IntegrationCallContext {

    public record CallMetadata(HttpMethod method, boolean idempotent, String idempotencyKey) {}

    private static final ThreadLocal<CallMetadata> CURRENT = new ThreadLocal<>();

    private IntegrationCallContext() {}

    public static void set(CallMetadata metadata) {
        CURRENT.set(metadata);
    }

    public static CallMetadata get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static CallMetadata forMethod(HttpMethod method, String idempotencyKey) {
        boolean idempotent =
                method == HttpMethod.GET
                        || method == HttpMethod.HEAD
                        || method == HttpMethod.OPTIONS
                        || method == HttpMethod.TRACE;
        boolean hasKey = idempotencyKey != null && !idempotencyKey.isBlank();
        return new CallMetadata(method, idempotent || hasKey, idempotencyKey);
    }

    public static boolean currentCallIsRetrySafe() {
        CallMetadata metadata = CURRENT.get();
        if (metadata == null) {
            return false;
        }
        if (metadata.idempotent()) {
            return true;
        }
        return metadata.idempotencyKey() != null && !metadata.idempotencyKey().isBlank();
    }

    /** Header name checked when no explicit context was set on the calling thread. */
    public static String idempotencyHeaderName() {
        return Headers.IDEMPOTENCY_KEY;
    }
}
