package com.telemedicine.auditservice.service;

import com.telemedicine.auditservice.config.AuditServiceProperties;
import com.telemedicine.auditservice.domain.AuditChainHeadEntity;
import com.telemedicine.auditservice.domain.AuditEventEntity;
import com.telemedicine.auditservice.domain.ConsentLedgerEntity;
import com.telemedicine.auditservice.export.MinioExportStorage;
import com.telemedicine.auditservice.repository.AuditChainHeadRepository;
import com.telemedicine.auditservice.repository.AuditEventRepository;
import com.telemedicine.auditservice.repository.ConsentLedgerRepository;
import com.telemedicine.auditservice.web.dto.AuditEventResponse;
import com.telemedicine.auditservice.web.dto.AuditExportRequest;
import com.telemedicine.auditservice.web.dto.AuditExportResponse;
import com.telemedicine.auditservice.web.dto.AuditIngestRequest;
import com.telemedicine.auditservice.web.dto.ChainVerifyResponse;
import com.telemedicine.auditservice.web.dto.ConsentLedgerResponse;
import com.telemedicine.auditservice.web.dto.RetentionRunResponse;
import com.telemedicine.common.audit.AuditHasher;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.AuditRecorded;
import com.telemedicine.common.events.payload.ChatMessageSent;
import com.telemedicine.common.events.payload.ConsentGranted;
import com.telemedicine.common.events.payload.ConsentWithdrawn;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.web.PageResponse;
import com.telemedicine.common.web.page.PageQuery;
import com.telemedicine.common.web.page.SortSpec;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;
    private final AuditChainHeadRepository chainHeadRepository;
    private final ConsentLedgerRepository consentLedgerRepository;
    private final MinioExportStorage exportStorage;
    private final AuditServiceProperties properties;
    private final Optional<OutboxRecorder> outboxRecorder;

    public AuditService(
            AuditEventRepository auditEventRepository,
            AuditChainHeadRepository chainHeadRepository,
            ConsentLedgerRepository consentLedgerRepository,
            MinioExportStorage exportStorage,
            AuditServiceProperties properties,
            Optional<OutboxRecorder> outboxRecorder) {
        this.auditEventRepository = auditEventRepository;
        this.chainHeadRepository = chainHeadRepository;
        this.consentLedgerRepository = consentLedgerRepository;
        this.exportStorage = exportStorage;
        this.properties = properties;
        this.outboxRecorder = outboxRecorder;
    }

    @Transactional
    public AuditEventResponse ingestHttp(AuditIngestRequest request) {
        String auditId = request.auditId() == null || request.auditId().isBlank()
                ? IdGenerator.newUlid()
                : request.auditId();
        if (auditEventRepository.existsById(auditId)) {
            return AuditMapper.toResponse(auditEventRepository.findById(auditId).orElseThrow());
        }

        Instant occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();
        String actorRole = blankToDefault(request.actorRole(), "SERVICE");

        AuditEventEntity saved =
                appendChained(
                        auditId,
                        request.actorId(),
                        actorRole,
                        request.action(),
                        request.resourceType(),
                        request.resourceId(),
                        request.outcome(),
                        occurredAt,
                        request.actorIp(),
                        request.userAgent(),
                        request.reason(),
                        request.correlationId(),
                        null,
                        null);

        publishAuditRecorded(saved);
        return AuditMapper.toResponse(saved);
    }

    @Transactional
    public Optional<AuditEventResponse> ingestRecorded(AuditRecorded recorded, String correlationId) {
        if (auditEventRepository.existsById(recorded.auditId())) {
            return auditEventRepository.findById(recorded.auditId()).map(AuditMapper::toResponse);
        }

        String expectedHash =
                AuditHasher.hash(
                        recorded.actorRef(),
                        recorded.action(),
                        recorded.resourceType(),
                        recorded.resourceRef(),
                        recorded.outcome(),
                        recorded.occurredAt(),
                        normalizeHash(recorded.previousHash()));

        if (!Objects.equals(expectedHash, recorded.hash())) {
            throw new DomainException(
                    ErrorCode.AUDIT_CHAIN_BROKEN,
                    "AuditRecorded hash mismatch for " + recorded.auditId());
        }

        AuditEventEntity saved =
                appendChained(
                        recorded.auditId(),
                        recorded.actorRef(),
                        "EVENT",
                        recorded.action(),
                        recorded.resourceType(),
                        recorded.resourceRef(),
                        recorded.outcome(),
                        recorded.occurredAt(),
                        null,
                        null,
                        null,
                        correlationId,
                        normalizeHash(recorded.previousHash()),
                        recorded.hash());

        return Optional.of(AuditMapper.toResponse(saved));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> query(
            String actorId,
            String action,
            String resourceType,
            Instant from,
            Instant to,
            PageQuery pageQuery) {
        Specification<AuditEventEntity> specification = filters(actorId, action, resourceType, from, to);
        Page<AuditEventEntity> page =
                auditEventRepository.findAll(specification, toPageable(pageQuery));
        List<AuditEventResponse> items = page.getContent().stream().map(AuditMapper::toResponse).toList();
        return PageResponse.of(items, pageQuery.page(), pageQuery.size(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ConsentLedgerResponse> queryConsent(String appointmentId) {
        return consentLedgerRepository.findByAppointmentIdOrderByRecordedAtDesc(appointmentId).stream()
                .map(AuditMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChainVerifyResponse verifyChain() {
        List<AuditEventEntity> events = auditEventRepository.findAllByOrderBySequenceAsc();
        String previousHash = "";
        long expectedSequence = 1L;

        for (AuditEventEntity event : events) {
            if (event.getSequence() != expectedSequence) {
                throw brokenChain(
                        "Sequence gap at "
                                + event.getId()
                                + ": expected "
                                + expectedSequence
                                + " got "
                                + event.getSequence());
            }
            if (!Objects.equals(normalizeHash(event.getPreviousHash()), previousHash)) {
                throw brokenChain("Previous hash mismatch at sequence " + event.getSequence());
            }

            String expectedHash =
                    AuditHasher.hash(
                            event.getActorId(),
                            event.getAction(),
                            event.getResourceType(),
                            event.getResourceId(),
                            event.getOutcome(),
                            event.getOccurredAt(),
                            previousHash);
            if (!Objects.equals(expectedHash, event.getHash())) {
                throw brokenChain("Hash mismatch at sequence " + event.getSequence());
            }

            previousHash = event.getHash();
            expectedSequence++;
        }

        AuditChainHeadEntity head = chainHead(AuditChainHeadEntity.GLOBAL_ID);
        if (events.isEmpty()) {
            return new ChainVerifyResponse(true, 0, head.getLastHash(), head.getLastSequence(), "empty chain");
        }

        AuditEventEntity last = events.get(events.size() - 1);
        if (last.getSequence() != head.getLastSequence() || !Objects.equals(last.getHash(), head.getLastHash())) {
            throw brokenChain("Chain head does not match the latest audit event");
        }

        return new ChainVerifyResponse(
                true,
                events.size(),
                head.getLastHash(),
                head.getLastSequence(),
                "chain intact");
    }

    @Transactional
    public AuditExportResponse export(AuditExportRequest request) {
        Instant from = request.from() == null ? Instant.EPOCH : request.from();
        Instant to = request.to() == null ? Instant.now() : request.to();
        List<AuditEventEntity> events =
                auditEventRepository.findAll(filters(request.actorId(), request.action(), null, from, to));

        String format = request.format() == null ? "json" : request.format().toLowerCase(Locale.ROOT);
        String objectKey = "exports/audit-" + IdGenerator.newUlid() + "." + format;
        String contentType = "json".equals(format) ? "application/json" : "text/csv";
        String body = "json".equals(format) ? toJson(events) : toCsv(events);

        MinioExportStorage.StoredObject stored =
                exportStorage.storeText(objectKey, contentType, body);
        return new AuditExportResponse(
                stored.objectKey(), stored.downloadUrl(), stored.expiresAt(), events.size());
    }

    @Transactional
    public RetentionRunResponse runRetention() {
        Instant cutoff = Instant.now().minus(properties.retentionDays(), ChronoUnit.DAYS);
        List<ConsentLedgerEntity> expired = consentLedgerRepository.findByRecordedAtBefore(cutoff);
        int archived = 0;
        int tombstoned = 0;

        for (ConsentLedgerEntity entry : expired) {
            String objectKey = "cold/consent/" + entry.getId() + ".json";
            exportStorage.storeText(objectKey, "application/json", consentJson(entry));
            archived++;
            consentLedgerRepository.delete(entry);
            tombstoned++;
        }

        long auditEventsRetained = auditEventRepository.count();
        log.info(
                "Retention run archived {} consent records; {} audit events retained",
                archived,
                auditEventsRetained);
        return new RetentionRunResponse(archived, tombstoned, auditEventsRetained);
    }

    @Transactional
    public void recordChatMessageSent(ChatMessageSent sent, String correlationId) {
        String auditId = "chat-" + sent.messageId();
        if (auditEventRepository.existsById(auditId)) {
            return;
        }
        appendChained(
                auditId,
                sent.senderRole(),
                sent.senderRole(),
                "CHAT_MESSAGE_SENT",
                "CHAT_MESSAGE",
                sent.messageId(),
                "SUCCESS",
                sent.sentAt(),
                null,
                null,
                null,
                correlationId,
                null,
                null);
    }

    @Transactional
    public void recordConsentGranted(ConsentGranted granted, String appointmentId) {
        if (consentLedgerRepository.existsById(granted.consentId())) {
            return;
        }
        consentLedgerRepository.save(
                new ConsentLedgerEntity(
                        granted.consentId(),
                        appointmentId,
                        granted.consentType(),
                        granted.version(),
                        true,
                        granted.grantedAt(),
                        granted.subjectRef(),
                        granted.consentType(),
                        null));
    }

    @Transactional
    public void recordConsentWithdrawn(ConsentWithdrawn withdrawn, String appointmentId) {
        if (consentLedgerRepository.existsById(withdrawn.consentId())) {
            return;
        }
        consentLedgerRepository.save(
                new ConsentLedgerEntity(
                        withdrawn.consentId(),
                        appointmentId,
                        withdrawn.consentType(),
                        "WITHDRAWN",
                        false,
                        withdrawn.withdrawnAt(),
                        withdrawn.subjectRef(),
                        withdrawn.consentType(),
                        withdrawn.withdrawnAt()));
    }

    private AuditEventEntity appendChained(
            String auditId,
            String actorId,
            String actorRole,
            String action,
            String resourceType,
            String resourceId,
            String outcome,
            Instant occurredAt,
            String actorIp,
            String userAgent,
            String reason,
            String correlationId,
            String expectedPreviousHash,
            String expectedHash) {
        AuditChainHeadEntity head = chainHead(AuditChainHeadEntity.GLOBAL_ID);
        String previousHash = head.getLastHash();
        long sequence = head.getLastSequence() + 1;

        if (expectedPreviousHash != null && !Objects.equals(expectedPreviousHash, previousHash)) {
            throw brokenChain(
                    "Incoming previous hash "
                            + expectedPreviousHash
                            + " does not match chain tip "
                            + previousHash);
        }

        String hash =
                expectedHash == null
                        ? AuditHasher.hash(
                                actorId,
                                action,
                                resourceType,
                                resourceId,
                                outcome,
                                occurredAt,
                                previousHash)
                        : expectedHash;

        AuditEventEntity entity =
                new AuditEventEntity(
                        auditId,
                        actorId,
                        actorRole,
                        action,
                        resourceType,
                        resourceId,
                        outcome,
                        occurredAt,
                        actorIp,
                        userAgent,
                        reason,
                        sequence,
                        previousHash,
                        hash,
                        correlationId);
        AuditEventEntity saved = auditEventRepository.save(entity);
        head.advance(sequence, hash, Instant.now());
        chainHeadRepository.save(head);
        return saved;
    }

    private AuditChainHeadEntity chainHead(String id) {
        return chainHeadRepository
                .findAndLockById(id)
                .orElseGet(
                        () ->
                                chainHeadRepository.save(
                                        new AuditChainHeadEntity(id, 0L, "", Instant.now())));
    }

    private void publishAuditRecorded(AuditEventEntity entity) {
        outboxRecorder.ifPresent(
                recorder ->
                        recorder.record(
                                "audit",
                                entity.getId(),
                                new AuditRecorded(
                                        entity.getId(),
                                        entity.getActorId(),
                                        entity.getAction(),
                                        entity.getResourceType(),
                                        entity.getResourceId(),
                                        entity.getOutcome(),
                                        entity.getPreviousHash(),
                                        entity.getHash(),
                                        entity.getOccurredAt()),
                                entity.getCorrelationId(),
                                entity.getId()));
    }

    private static Specification<AuditEventEntity> filters(
            String actorId, String action, String resourceType, Instant from, Instant to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actorId != null && !actorId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("actorId"), actorId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (resourceType != null && !resourceType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceType"), resourceType));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Pageable toPageable(PageQuery pageQuery) {
        Sort sort =
                pageQuery.isSorted()
                        ? Sort.by(
                                pageQuery.sort().stream()
                                        .map(AuditService::toOrder)
                                        .toList())
                        : Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("sequence"));
        return PageRequest.of(pageQuery.page(), pageQuery.size(), sort);
    }

    private static Sort.Order toOrder(SortSpec spec) {
        return spec.descending() ? Sort.Order.desc(spec.field()) : Sort.Order.asc(spec.field());
    }

    private static String normalizeHash(String hash) {
        return hash == null ? "" : hash;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static DomainException brokenChain(String detail) {
        return new DomainException(ErrorCode.AUDIT_CHAIN_BROKEN, detail);
    }

    private static String toJson(List<AuditEventEntity> events) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < events.size(); index++) {
            AuditEventEntity event = events.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append('{')
                    .append("\"auditId\":\"")
                    .append(escape(event.getId()))
                    .append("\",")
                    .append("\"actorId\":\"")
                    .append(escape(event.getActorId()))
                    .append("\",")
                    .append("\"action\":\"")
                    .append(escape(event.getAction()))
                    .append("\",")
                    .append("\"resourceType\":\"")
                    .append(escape(event.getResourceType()))
                    .append("\",")
                    .append("\"resourceId\":\"")
                    .append(escape(event.getResourceId()))
                    .append("\",")
                    .append("\"outcome\":\"")
                    .append(escape(event.getOutcome()))
                    .append("\",")
                    .append("\"occurredAt\":\"")
                    .append(event.getOccurredAt())
                    .append("\",")
                    .append("\"sequence\":")
                    .append(event.getSequence())
                    .append(',')
                    .append("\"hash\":\"")
                    .append(escape(event.getHash()))
                    .append("\"")
                    .append('}');
        }
        return builder.append(']').toString();
    }

    private static String toCsv(List<AuditEventEntity> events) {
        StringBuilder builder =
                new StringBuilder(
                        "auditId,actorId,actorRole,action,resourceType,resourceId,outcome,occurredAt,sequence,hash\n");
        for (AuditEventEntity event : events) {
            builder.append(csv(event.getId()))
                    .append(',')
                    .append(csv(event.getActorId()))
                    .append(',')
                    .append(csv(event.getActorRole()))
                    .append(',')
                    .append(csv(event.getAction()))
                    .append(',')
                    .append(csv(event.getResourceType()))
                    .append(',')
                    .append(csv(event.getResourceId()))
                    .append(',')
                    .append(csv(event.getOutcome()))
                    .append(',')
                    .append(event.getOccurredAt())
                    .append(',')
                    .append(event.getSequence())
                    .append(',')
                    .append(csv(event.getHash()))
                    .append('\n');
        }
        return builder.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String consentJson(ConsentLedgerEntity entry) {
        return "{"
                + "\"consentId\":\""
                + escape(entry.getId())
                + "\","
                + "\"appointmentId\":\""
                + escape(entry.getAppointmentId())
                + "\","
                + "\"subjectRef\":\""
                + escape(entry.getSubjectRef())
                + "\","
                + "\"consentType\":\""
                + escape(entry.getConsentType())
                + "\","
                + "\"version\":\""
                + escape(entry.getVersion())
                + "\","
                + "\"granted\":"
                + entry.isGranted()
                + ","
                + "\"recordedAt\":\""
                + entry.getRecordedAt()
                + "\""
                + "}";
    }
}
