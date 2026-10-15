package com.telemedicine.common.web.idempotency;

import java.time.Instant;

/**
 * The stored outcome of a keyed request.
 *
 * @param key the client-supplied idempotency key
 * @param fingerprint hash of method, path and body, used to detect a key reused for a different
 *     request
 * @param status HTTP status of the completed response, or 0 while still in flight
 * @param responseBody serialised response body, or null while still in flight
 * @param contentType media type of the stored body
 * @param completed whether the original request finished
 * @param createdAt when the key was first reserved
 */
public record IdempotencyRecord(
        String key,
        String fingerprint,
        int status,
        String responseBody,
        String contentType,
        boolean completed,
        Instant createdAt) {

    public static IdempotencyRecord inFlight(String key, String fingerprint, Instant now) {
        return new IdempotencyRecord(key, fingerprint, 0, null, null, false, now);
    }

    public IdempotencyRecord complete(int newStatus, String body, String newContentType) {
        return new IdempotencyRecord(
                key, fingerprint, newStatus, body, newContentType, true, createdAt);
    }
}
