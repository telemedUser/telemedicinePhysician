package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * An auditable action was recorded, carrying the hash-chain link that makes the trail tamper
 * evident.
 *
 * @param auditId identifier of the audit entry
 * @param actorRef pseudonymous reference to who acted
 * @param action coded action, for example {@code DOCUMENT_VIEWED}
 * @param resourceType type of resource acted on
 * @param resourceRef pseudonymous reference to the specific resource
 * @param outcome {@code SUCCESS}, {@code FAILURE} or {@code DENIED}
 * @param previousHash hash of the preceding entry, null for the first in a chain
 * @param hash hash of this entry over its canonical form
 * @param occurredAt when the action happened
 */
public record AuditRecorded(
        String auditId,
        String actorRef,
        String action,
        String resourceType,
        String resourceRef,
        String outcome,
        String previousHash,
        String hash,
        Instant occurredAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.AUDIT_RECORDED;
    }
}
