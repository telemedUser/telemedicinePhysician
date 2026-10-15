package com.telemedicine.waitingroomservice.consent;

import com.telemedicine.common.audit.AuditChainState;
import com.telemedicine.common.audit.AuditEventDispatcher;
import com.telemedicine.common.audit.ConsentAuditEvents;
import com.telemedicine.common.audit.TelemedicineAuditProperties;
import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.core.error.ConsentRequiredException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.consent.ConsentDecision;
import com.telemedicine.common.security.consent.ConsentVerifier;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.domain.ConsentEntity;
import com.telemedicine.waitingroomservice.domain.ConsentType;
import com.telemedicine.waitingroomservice.repository.ConsentRepository;
import com.telemedicine.waitingroomservice.web.dto.ConsentGrantRequest;
import com.telemedicine.waitingroomservice.web.dto.ConsentGrantResponse;
import com.telemedicine.waitingroomservice.web.dto.ConsentStatusResponse;
import com.telemedicine.waitingroomservice.web.dto.ConsentWithdrawRequest;
import com.telemedicine.waitingroomservice.web.dto.ConsentWithdrawResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService implements ConsentVerifier {

    private final ConsentRepository consentRepository;
    private final OutboxRecorder outboxRecorder;
    private final AuditEventDispatcher auditDispatcher;
    private final AuditChainState auditChainState;
    private final SecurityContextAccessor securityContext;
    private final TelemedicineAuditProperties auditProperties;
    private final WaitingRoomServiceProperties properties;

    public ConsentService(
            ConsentRepository consentRepository,
            OutboxRecorder outboxRecorder,
            AuditEventDispatcher auditDispatcher,
            AuditChainState auditChainState,
            SecurityContextAccessor securityContext,
            TelemedicineAuditProperties auditProperties,
            WaitingRoomServiceProperties properties) {
        this.consentRepository = consentRepository;
        this.outboxRecorder = outboxRecorder;
        this.auditDispatcher = auditDispatcher;
        this.auditChainState = auditChainState;
        this.securityContext = securityContext;
        this.auditProperties = auditProperties;
        this.properties = properties;
    }

    @Transactional
    public ConsentGrantResponse grantPlatform(ConsentGrantRequest request) {
        return grant(request, ConsentType.PLATFORM_TERMS, properties.getPlatformConsentVersion());
    }

    @Transactional
    public ConsentGrantResponse grantClinical(ConsentGrantRequest request) {
        return grant(request, ConsentType.CLINICAL_INFORMED, properties.getClinicalConsentVersion());
    }

    @Transactional
    public ConsentWithdrawResponse withdraw(ConsentWithdrawRequest request) {
        String subjectId = resolveSubjectId(request.appointmentId());
        List<ConsentEntity> activeConsents =
                consentRepository.findByAppointmentIdAndWithdrawnAtIsNull(request.appointmentId());
        if (activeConsents.isEmpty()) {
            throw new ConflictException(ErrorCode.CONSENT_REQUIRED, "No active consent to withdraw");
        }
        Instant withdrawnAt = Instant.now();
        for (ConsentEntity consent : activeConsents) {
            consent.setWithdrawnAt(withdrawnAt);
            consentRepository.save(consent);
            ConsentAuditEvents.recordWithdrawn(
                    outboxRecorder,
                    auditDispatcher,
                    auditChainState,
                    securityContext,
                    auditProperties,
                    consent.getId(),
                    consent.getSubjectId(),
                    consent.getConsentType(),
                    consent.getAppointmentId(),
                    withdrawnAt);
        }
        return new ConsentWithdrawResponse(request.appointmentId(), "WITHDRAWN");
    }

    public ConsentStatusResponse status(String subjectId, String type, String version) {
        ConsentType consentType = ConsentType.fromWire(type);
        ConsentDecision decision = verify(subjectId, consentType.name(), blankToNull(version));
        if (decision.granted()) {
            return ConsentStatusResponse.consented();
        }
        return switch (decision.reason()) {
            case CONSENT_WITHDRAWN -> ConsentStatusResponse.withdrawn();
            case CONSENT_VERSION_OUTDATED ->
                    ConsentStatusResponse.outdated(decision.requiredVersion());
            default -> ConsentStatusResponse.missing();
        };
    }

    @Override
    public ConsentDecision verify(String subjectId, String consentType, String requiredVersion) {
        return consentRepository
                .findActiveBySubjectAndType(subjectId, consentType)
                .map(
                        consent -> {
                            if (requiredVersion != null
                                    && !requiredVersion.isBlank()
                                    && !requiredVersion.equals(consent.getVersion())) {
                                return ConsentDecision.outdated(requiredVersion);
                            }
                            return ConsentDecision.grant();
                        })
                .orElseGet(() -> latestWithdrawn(subjectId, consentType));
    }

    private ConsentDecision latestWithdrawn(String subjectId, String consentType) {
        return consentRepository.findHistoryBySubjectAndType(subjectId, consentType).stream()
                .findFirst()
                .filter(consent -> consent.getWithdrawnAt() != null)
                .map(ignored -> ConsentDecision.withdrawn())
                .orElseGet(ConsentDecision::missing);
    }

    public void requireBothConsents(String subjectId) {
        requireConsent(subjectId, ConsentType.PLATFORM_TERMS.name(), properties.getPlatformConsentVersion());
        requireConsent(subjectId, ConsentType.CLINICAL_INFORMED.name(), properties.getClinicalConsentVersion());
    }

    public void requireConsent(String subjectId, String consentType, String requiredVersion) {
        ConsentDecision decision = verify(subjectId, consentType, requiredVersion);
        if (decision.granted()) {
            return;
        }
        throw switch (decision.reason()) {
            case CONSENT_WITHDRAWN -> ConsentRequiredException.withdrawn(consentType);
            case CONSENT_VERSION_OUTDATED ->
                    ConsentRequiredException.outdated(consentType, decision.requiredVersion());
            default -> new ConsentRequiredException(consentType);
        };
    }

    private ConsentGrantResponse grant(
            ConsentGrantRequest request, ConsentType consentType, String defaultVersion) {
        String subjectId = resolveSubjectId(request.appointmentId());
        String version =
                request.version() == null || request.version().isBlank()
                        ? defaultVersion
                        : request.version();
        Instant grantedAt = Instant.now();
        String consentId = ConsentAuditEvents.newConsentId();

        ConsentEntity entity = new ConsentEntity();
        entity.setId(consentId);
        entity.setSubjectId(subjectId);
        entity.setAppointmentId(request.appointmentId());
        entity.setConsentType(consentType.name());
        entity.setVersion(version);
        entity.setGrantedAt(grantedAt);
        entity.setCreatedAt(grantedAt);
        consentRepository.save(entity);

        ConsentAuditEvents.recordGranted(
                outboxRecorder,
                auditDispatcher,
                auditChainState,
                securityContext,
                auditProperties,
                consentId,
                subjectId,
                consentType.name(),
                version,
                request.appointmentId(),
                grantedAt);

        return new ConsentGrantResponse(
                request.appointmentId(), consentType.name(), version, "GRANTED");
    }

    private String resolveSubjectId(String appointmentId) {
        return securityContext
                .currentPatientId()
                .orElseGet(() -> securityContext.currentUserId().orElse(appointmentId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
