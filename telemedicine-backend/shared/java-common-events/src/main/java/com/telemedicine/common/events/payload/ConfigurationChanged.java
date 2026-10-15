package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * A configuration entry changed, for example the active video provider.
 *
 * <p>Values travel as hashes. Configuration holds credentials and provider secrets, and a change
 * event is broadcast to every service, so publishing the value itself would spread them.
 *
 * @param key configuration key
 * @param scope scope the key applies to, for example a service name or {@code GLOBAL}
 * @param previousHash SHA-256 of the previous value, null when the key is new
 * @param newHash SHA-256 of the new value
 * @param changedBy pseudonymous reference to the actor who made the change
 * @param changedAt when the change took effect
 */
public record ConfigurationChanged(
        String key,
        String scope,
        String previousHash,
        String newHash,
        String changedBy,
        Instant changedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.CONFIGURATION_CHANGED;
    }
}
