package com.telemedicine.common.audit;

import java.time.Instant;

/**
 * Immutable audit record with a tamper-evident hash chain link.
 *
 * @param auditId unique identifier for this entry
 * @param actor who performed the action
 * @param action coded action, for example {@code DOCUMENT_VIEWED}
 * @param resourceType type of resource acted on
 * @param resourceId pseudonymous reference to the specific resource
 * @param outcome whether the action succeeded, failed or was denied
 * @param reason optional human-readable reason, PHI-redacted before persistence
 * @param beforeHash SHA-256 of the pre-change state when {@code includeArgs} captured one
 * @param afterHash SHA-256 of the post-change state when captured
 * @param correlationId request or journey correlation identifier
 * @param occurredAt when the action happened
 * @param sequence monotonic sequence within the local chain
 * @param previousHash hash of the preceding entry, empty for the first link
 * @param hash SHA-256 over the canonical form of this entry
 */
public record AuditEvent(
        String auditId,
        AuditActor actor,
        String action,
        String resourceType,
        String resourceId,
        AuditOutcome outcome,
        String reason,
        String beforeHash,
        String afterHash,
        String correlationId,
        Instant occurredAt,
        long sequence,
        String previousHash,
        String hash) {

    /** Convenience accessor matching legacy flat payloads. */
    public String actorId() {
        return actor.id();
    }

    /** Convenience accessor matching legacy flat payloads. */
    public String actorRole() {
        return actor.role();
    }

    /** String form of {@link #outcome()} for Kafka and persistence consumers. */
    public String outcomeName() {
        return outcome.name();
    }
}
