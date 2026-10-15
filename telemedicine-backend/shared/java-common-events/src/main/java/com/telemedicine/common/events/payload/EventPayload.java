package com.telemedicine.common.events.payload;

/**
 * Body of a domain event.
 *
 * <p>Implementations are records carrying identifiers, codes, timestamps and status values only.
 * They must not carry PHI: an event body crosses service and topic boundaries, is retained by the
 * broker, is copied into dead-letter topics and is read by anyone debugging a consumer. A test in
 * this module enforces the rule structurally rather than by review.
 *
 * <p>Each implementation declares the catalogue type it belongs to, so a publisher cannot pair a
 * payload with the wrong event type.
 */
public interface EventPayload {

    /** Catalogue type from {@link com.telemedicine.common.events.EventTypes}. */
    String eventType();

    /** Schema version of this payload. */
    default String schemaVersion() {
        return "1.0";
    }
}
