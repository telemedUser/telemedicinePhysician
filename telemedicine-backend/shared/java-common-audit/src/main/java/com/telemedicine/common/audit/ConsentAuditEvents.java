package com.telemedicine.common.audit;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.core.phi.PhiRedactor;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.payload.ConsentGranted;
import com.telemedicine.common.events.payload.ConsentWithdrawn;
import com.telemedicine.common.events.payload.EventPayload;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.time.Instant;
import org.slf4j.MDC;

/** Helpers for consent ledger audit events. */
public final class ConsentAuditEvents {

    public static final String CONSENT_GRANTED_ACTION = "CONSENT_GRANTED";
    public static final String CONSENT_WITHDRAWN_ACTION = "CONSENT_WITHDRAWN";
    public static final String CONSENT_RESOURCE_TYPE = "CONSENT";

    private ConsentAuditEvents() {}

    public static ConsentGranted granted(
            String consentId,
            String subjectRef,
            String consentType,
            String version,
            Instant grantedAt) {
        return new ConsentGranted(consentId, subjectRef, consentType, version, grantedAt);
    }

    public static ConsentWithdrawn withdrawn(
            String consentId, String subjectRef, String consentType, Instant withdrawnAt) {
        return new ConsentWithdrawn(consentId, subjectRef, consentType, withdrawnAt);
    }

    public static void recordGranted(
            OutboxRecorder outboxRecorder,
            AuditEventDispatcher auditDispatcher,
            AuditChainState chainState,
            SecurityContextAccessor securityContext,
            TelemedicineAuditProperties properties,
            String consentId,
            String subjectRef,
            String consentType,
            String version,
            String appointmentId,
            Instant grantedAt) {
        ConsentGranted payload = granted(consentId, subjectRef, consentType, version, grantedAt);
        recordConsentEvent(outboxRecorder, appointmentId, payload, correlationId());
        auditDispatcher.dispatch(
                consentAuditEvent(
                        chainState,
                        securityContext,
                        properties,
                        CONSENT_GRANTED_ACTION,
                        consentId,
                        AuditOutcome.SUCCESS,
                        grantedAt,
                        "consentType=" + consentType + ",version=" + version));
    }

    public static void recordWithdrawn(
            OutboxRecorder outboxRecorder,
            AuditEventDispatcher auditDispatcher,
            AuditChainState chainState,
            SecurityContextAccessor securityContext,
            TelemedicineAuditProperties properties,
            String consentId,
            String subjectRef,
            String consentType,
            String appointmentId,
            Instant withdrawnAt) {
        ConsentWithdrawn payload = withdrawn(consentId, subjectRef, consentType, withdrawnAt);
        recordConsentEvent(outboxRecorder, appointmentId, payload, correlationId());
        auditDispatcher.dispatch(
                consentAuditEvent(
                        chainState,
                        securityContext,
                        properties,
                        CONSENT_WITHDRAWN_ACTION,
                        consentId,
                        AuditOutcome.SUCCESS,
                        withdrawnAt,
                        "consentType=" + consentType));
    }

    public static void recordConsentEvent(
            OutboxRecorder outboxRecorder,
            String appointmentId,
            EventPayload payload,
            String correlationId) {
        outboxRecorder.record("consent", appointmentId, payload, correlationId, appointmentId);
    }

    public static String newConsentId() {
        return IdGenerator.newId();
    }

    public static String grantedEventType() {
        return EventTypes.CONSENT_GRANTED;
    }

    public static String withdrawnEventType() {
        return EventTypes.CONSENT_WITHDRAWN;
    }

    private static AuditEvent consentAuditEvent(
            AuditChainState chainState,
            SecurityContextAccessor securityContext,
            TelemedicineAuditProperties properties,
            String action,
            String consentId,
            AuditOutcome outcome,
            Instant occurredAt,
            String reason) {
        AuditActor actor = resolveActor(securityContext, properties);
        AuditChainState.ChainLink link = chainState.nextLink();
        String redactedReason = PhiRedactor.redact(reason);
        String hash =
                AuditHasher.hash(
                        actor.id(),
                        action,
                        CONSENT_RESOURCE_TYPE,
                        consentId,
                        outcome.name(),
                        occurredAt,
                        link.previousHash());
        AuditEvent event =
                new AuditEvent(
                        IdGenerator.newId(),
                        actor,
                        action,
                        CONSENT_RESOURCE_TYPE,
                        consentId,
                        outcome,
                        redactedReason,
                        null,
                        null,
                        correlationId(),
                        occurredAt,
                        link.sequence(),
                        link.previousHash(),
                        hash);
        chainState.advance(hash);
        return event;
    }

    private static AuditActor resolveActor(
            SecurityContextAccessor securityContext, TelemedicineAuditProperties properties) {
        if (securityContext.isAuthenticated()) {
            return new AuditActor(
                    securityContext.currentUserId().orElse(properties.getSystemActorId()),
                    primaryRole(securityContext),
                    null,
                    null);
        }
        return AuditActor.system(properties.getSystemActorRole());
    }

    private static String primaryRole(SecurityContextAccessor securityContext) {
        if (securityContext.hasRole(com.telemedicine.common.security.Roles.REALM_ADMIN)) {
            return com.telemedicine.common.security.Roles.REALM_ADMIN;
        }
        if (securityContext.hasRole(com.telemedicine.common.security.Roles.REALM_PHYSICIAN)) {
            return com.telemedicine.common.security.Roles.REALM_PHYSICIAN;
        }
        if (securityContext.hasRole(com.telemedicine.common.security.Roles.REALM_PATIENT)) {
            return com.telemedicine.common.security.Roles.REALM_PATIENT;
        }
        if (securityContext.hasRole(com.telemedicine.common.security.Roles.REALM_SERVICE)) {
            return com.telemedicine.common.security.Roles.REALM_SERVICE;
        }
        return "SYSTEM";
    }

    private static String correlationId() {
        String fromMdc = MDC.get(MdcKeys.CORRELATION_ID);
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }
}
