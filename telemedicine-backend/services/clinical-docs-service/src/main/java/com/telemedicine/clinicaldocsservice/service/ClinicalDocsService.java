package com.telemedicine.clinicaldocsservice.service;

import com.telemedicine.clinicaldocsservice.adapter.MedipassAdapter;
import com.telemedicine.clinicaldocsservice.adapter.MedipassCertificate;
import com.telemedicine.clinicaldocsservice.adapter.RecMedAdapter;
import com.telemedicine.clinicaldocsservice.adapter.RecMedPrescription;
import com.telemedicine.clinicaldocsservice.adapter.SimplePdfGenerator;
import com.telemedicine.clinicaldocsservice.api.dto.AppointmentDocumentsResponse;
import com.telemedicine.clinicaldocsservice.api.dto.IssueDocumentRequest;
import com.telemedicine.clinicaldocsservice.api.dto.IssueMedicalLeaveRequest;
import com.telemedicine.clinicaldocsservice.api.dto.IssuePrescriptionRequest;
import com.telemedicine.clinicaldocsservice.config.ClinicalDocsServiceProperties;
import com.telemedicine.clinicaldocsservice.domain.DocumentEntity;
import com.telemedicine.clinicaldocsservice.domain.DownloadTokenEntity;
import com.telemedicine.clinicaldocsservice.domain.MedicalLeaveEntity;
import com.telemedicine.clinicaldocsservice.domain.PrescriptionEntity;
import com.telemedicine.clinicaldocsservice.repository.DocumentRepository;
import com.telemedicine.clinicaldocsservice.repository.DownloadTokenRepository;
import com.telemedicine.clinicaldocsservice.repository.MedicalLeaveRepository;
import com.telemedicine.clinicaldocsservice.repository.PrescriptionRepository;
import com.telemedicine.clinicaldocsservice.storage.ClinicalDocumentStorage;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.ForbiddenException;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.ClinicalDocumentIssued;
import com.telemedicine.common.events.payload.MedicalLeaveIssued;
import com.telemedicine.common.events.payload.PrescriptionIssued;
import com.telemedicine.common.integration.ExternalSystems;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalDocsService {

    private static final String RESOURCE_DOCUMENT = "DOCUMENT";

    private final DocumentRepository documentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalLeaveRepository medicalLeaveRepository;
    private final DownloadTokenRepository downloadTokenRepository;
    private final ClinicalDocumentStorage storage;
    private final RecMedAdapter recMedAdapter;
    private final MedipassAdapter medipassAdapter;
    private final OutboxRecorder outboxRecorder;
    private final JourneyMetrics journeyMetrics;
    private final ClinicalDocsServiceProperties properties;
    private final SecurityContextAccessor securityContext;

    public ClinicalDocsService(
            DocumentRepository documentRepository,
            PrescriptionRepository prescriptionRepository,
            MedicalLeaveRepository medicalLeaveRepository,
            DownloadTokenRepository downloadTokenRepository,
            ClinicalDocumentStorage storage,
            RecMedAdapter recMedAdapter,
            MedipassAdapter medipassAdapter,
            OutboxRecorder outboxRecorder,
            JourneyMetrics journeyMetrics,
            ClinicalDocsServiceProperties properties,
            SecurityContextAccessor securityContext) {
        this.documentRepository = documentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicalLeaveRepository = medicalLeaveRepository;
        this.downloadTokenRepository = downloadTokenRepository;
        this.storage = storage;
        this.recMedAdapter = recMedAdapter;
        this.medipassAdapter = medipassAdapter;
        this.outboxRecorder = outboxRecorder;
        this.journeyMetrics = journeyMetrics;
        this.properties = properties;
        this.securityContext = securityContext;
    }

    @Transactional
    public Map<String, String> issueDocument(IssueDocumentRequest request) {
        Instant started = Instant.now();
        String physicianRef = currentPhysicianRef();
        String documentId = IdGenerator.newUlid();
        String objectKey = request.appointmentId() + "/" + documentId + ".pdf";
        byte[] pdf =
                SimplePdfGenerator.generate(
                        request.documentType(),
                        "Appointment: "
                                + request.appointmentId()
                                + "\nDiagnosis: "
                                + nullSafe(request.diagnosis()));
        storage.storeClinicalDocument(objectKey, pdf);

        DocumentEntity document = new DocumentEntity();
        document.setId(documentId);
        document.setAppointmentId(request.appointmentId());
        document.setDocumentType(request.documentType());
        document.setDiagnosis(request.diagnosis());
        document.setStorageKey(objectKey);
        document.setBucket(properties.getClinicalDocumentsBucket());
        document.setPhysicianRef(physicianRef);
        document.setIssuedAt(Instant.now());
        documentRepository.save(document);

        Instant issuedAt = document.getIssuedAt();
        outboxRecorder.record(
                "Document",
                documentId,
                new ClinicalDocumentIssued(
                        request.appointmentId(),
                        documentId,
                        request.documentType(),
                        physicianRef,
                        issuedAt),
                correlationId(),
                request.appointmentId());
        journeyMetrics.recordDocumentIssuanceTime(
                Duration.between(started, issuedAt), request.documentType());

        return Map.of("documentId", documentId, "event", "ClinicalDocumentIssued");
    }

    @Transactional
    public Map<String, String> issuePrescription(String appointmentId, IssuePrescriptionRequest request) {
        String physicianRef = currentPhysicianRef();
        RecMedPrescription external =
                recMedAdapter.issuePrescription(appointmentId, request.medications());

        String prescriptionId = IdGenerator.newUlid();
        String objectKey = appointmentId + "/" + prescriptionId + ".pdf";
        byte[] pdf =
                SimplePdfGenerator.generate(
                        "Prescription",
                        "Appointment: "
                                + appointmentId
                                + "\nMedications: "
                                + request.medications());
        storage.storePrescription(objectKey, pdf);

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setId(prescriptionId);
        prescription.setAppointmentId(appointmentId);
        prescription.setExternalPrescriptionId(external.prescriptionId());
        prescription.setProvider(ExternalSystems.RECMED);
        prescription.setMedications(request.medications());
        prescription.setStorageKey(objectKey);
        prescription.setBucket(properties.getPrescriptionsBucket());
        prescription.setPhysicianRef(physicianRef);
        prescription.setIssuedAt(Instant.now());
        prescriptionRepository.save(prescription);

        outboxRecorder.record(
                "Prescription",
                prescriptionId,
                new PrescriptionIssued(
                        appointmentId,
                        prescriptionId,
                        ExternalSystems.RECMED,
                        physicianRef,
                        prescription.getIssuedAt()),
                correlationId(),
                appointmentId);

        return Map.of(
                "prescriptionId", prescriptionId,
                "externalPrescriptionId", external.prescriptionId(),
                "event", "PrescriptionIssued");
    }

    @Transactional
    public Map<String, String> issueMedicalLeave(String appointmentId, IssueMedicalLeaveRequest request) {
        String physicianRef = currentPhysicianRef();
        MedipassCertificate external =
                medipassAdapter.issueCertificate(
                        appointmentId, request.durationDays(), request.diagnosis());

        String medicalLeaveId = IdGenerator.newUlid();
        String objectKey = appointmentId + "/" + medicalLeaveId + ".pdf";
        byte[] pdf =
                SimplePdfGenerator.generate(
                        "Medical Leave",
                        "Appointment: "
                                + appointmentId
                                + "\nDuration days: "
                                + request.durationDays());
        storage.storeClinicalDocument(objectKey, pdf);

        MedicalLeaveEntity medicalLeave = new MedicalLeaveEntity();
        medicalLeave.setId(medicalLeaveId);
        medicalLeave.setAppointmentId(appointmentId);
        medicalLeave.setExternalCertificateId(external.certificateId());
        medicalLeave.setProvider(ExternalSystems.MEDIPASS);
        medicalLeave.setDurationDays(request.durationDays());
        medicalLeave.setDiagnosis(request.diagnosis());
        medicalLeave.setStorageKey(objectKey);
        medicalLeave.setBucket(properties.getClinicalDocumentsBucket());
        medicalLeave.setPhysicianRef(physicianRef);
        medicalLeave.setIssuedAt(Instant.now());
        medicalLeaveRepository.save(medicalLeave);

        outboxRecorder.record(
                "MedicalLeave",
                medicalLeaveId,
                new MedicalLeaveIssued(
                        appointmentId,
                        medicalLeaveId,
                        ExternalSystems.MEDIPASS,
                        physicianRef,
                        request.durationDays(),
                        medicalLeave.getIssuedAt()),
                correlationId(),
                appointmentId);

        return Map.of(
                "medicalLeaveId", medicalLeaveId,
                "externalCertificateId", external.certificateId(),
                "event", "MedicalLeaveIssued");
    }

    @Transactional(readOnly = true)
    public AppointmentDocumentsResponse listForAppointment(String appointmentId) {
        List<DocumentEntity> documents =
                documentRepository.findByAppointmentIdOrderByIssuedAtDesc(appointmentId);
        List<PrescriptionEntity> prescriptions =
                prescriptionRepository.findByAppointmentIdOrderByIssuedAtDesc(appointmentId);
        List<MedicalLeaveEntity> medicalLeaves =
                medicalLeaveRepository.findByAppointmentIdOrderByIssuedAtDesc(appointmentId);

        String downloadUrl = null;
        if (!documents.isEmpty()) {
            downloadUrl = createDownloadUrl(documents.getFirst());
        }

        return new AppointmentDocumentsResponse(
                appointmentId,
                downloadUrl,
                documents.stream()
                        .map(
                                document ->
                                        new AppointmentDocumentsResponse.DocumentSummary(
                                                document.getId(),
                                                document.getDocumentType(),
                                                document.getIssuedAt().toString()))
                        .toList(),
                prescriptions.stream()
                        .map(
                                prescription ->
                                        new AppointmentDocumentsResponse.PrescriptionSummary(
                                                prescription.getId(),
                                                prescription.getProvider(),
                                                prescription.getIssuedAt().toString()))
                        .toList(),
                medicalLeaves.stream()
                        .map(
                                leave ->
                                        new AppointmentDocumentsResponse.MedicalLeaveSummary(
                                                leave.getId(),
                                                leave.getDurationDays(),
                                                leave.getIssuedAt().toString()))
                        .toList());
    }

    @Transactional
    public byte[] download(String token) {
        DownloadTokenEntity tokenEntity =
                downloadTokenRepository
                        .findByTokenHash(hashToken(token))
                        .orElseThrow(
                                () ->
                                        new ForbiddenException(
                                                ErrorCode.DOCUMENT_ACCESS_DENIED, "Invalid download token"));
        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new ForbiddenException(ErrorCode.DOCUMENT_ACCESS_DENIED, "Download token expired");
        }
        DocumentEntity document =
                documentRepository
                        .findById(tokenEntity.getResourceId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.DOCUMENT_NOT_FOUND, tokenEntity.getResourceId()));
        tokenEntity.setRedeemedAt(Instant.now());
        downloadTokenRepository.save(tokenEntity);
        return storage.read(document.getBucket(), document.getStorageKey());
    }

    private String createDownloadUrl(DocumentEntity document) {
        String rawToken = UUID.randomUUID().toString();
        DownloadTokenEntity tokenEntity = new DownloadTokenEntity();
        tokenEntity.setId(IdGenerator.newUlid());
        tokenEntity.setTokenHash(hashToken(rawToken));
        tokenEntity.setResourceType(RESOURCE_DOCUMENT);
        tokenEntity.setResourceId(document.getId());
        tokenEntity.setExpiresAt(Instant.now().plus(properties.getDownloadTokenTtl()));
        downloadTokenRepository.save(tokenEntity);
        return properties.getPublicBaseUrl() + "/api/v1/clinical-docs/download/" + rawToken;
    }

    private String currentPhysicianRef() {
        return securityContext.currentPhysicianId().orElse("physician-unknown");
    }

    private static String correlationId() {
        String fromMdc = MDC.get("correlationId");
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
