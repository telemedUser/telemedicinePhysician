package com.telemedicine.clinicaldocsservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.clinicaldocsservice.adapter.MedipassAdapter;
import com.telemedicine.clinicaldocsservice.adapter.RecMedAdapter;
import com.telemedicine.clinicaldocsservice.api.dto.IssueDocumentRequest;
import com.telemedicine.clinicaldocsservice.config.ClinicalDocsServiceProperties;
import com.telemedicine.clinicaldocsservice.domain.DocumentEntity;
import com.telemedicine.clinicaldocsservice.repository.DocumentRepository;
import com.telemedicine.clinicaldocsservice.repository.DownloadTokenRepository;
import com.telemedicine.clinicaldocsservice.repository.MedicalLeaveRepository;
import com.telemedicine.clinicaldocsservice.repository.PrescriptionRepository;
import com.telemedicine.clinicaldocsservice.storage.ClinicalDocumentStorage;
import com.telemedicine.common.events.payload.ClinicalDocumentIssued;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClinicalDocsServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private MedicalLeaveRepository medicalLeaveRepository;
    @Mock private DownloadTokenRepository downloadTokenRepository;
    @Mock private ClinicalDocumentStorage storage;
    @Mock private RecMedAdapter recMedAdapter;
    @Mock private MedipassAdapter medipassAdapter;
    @Mock private OutboxRecorder outboxRecorder;
    @Mock private SecurityContextAccessor securityContext;

    private ClinicalDocsService clinicalDocsService;

    @BeforeEach
    void setUp() {
        ClinicalDocsServiceProperties properties = new ClinicalDocsServiceProperties();
        properties.setPublicBaseUrl("http://clinical-docs");
        properties.setDownloadTokenTtl(Duration.ofHours(1));
        clinicalDocsService =
                new ClinicalDocsService(
                        documentRepository,
                        prescriptionRepository,
                        medicalLeaveRepository,
                        downloadTokenRepository,
                        storage,
                        recMedAdapter,
                        medipassAdapter,
                        outboxRecorder,
                        new JourneyMetrics(new SimpleMeterRegistry()),
                        properties,
                        securityContext);
    }

    @Test
    void issueDocumentStoresPdfAndEmitsEvent() {
        when(securityContext.currentPhysicianId()).thenReturn(Optional.of("phy-1"));

        var response =
                clinicalDocsService.issueDocument(
                        new IssueDocumentRequest("apt-1", "CONSULTATION_SUMMARY", "Hypertension"));

        assertThat(response.get("event")).isEqualTo("ClinicalDocumentIssued");
        verify(storage).storeClinicalDocument(any(), any());
        verify(documentRepository).save(any(DocumentEntity.class));
        verify(outboxRecorder)
                .record(eq("Document"), any(), any(ClinicalDocumentIssued.class), any(), eq("apt-1"));
    }

    @Test
    void listForAppointmentReturnsDownloadUrlWhenDocumentExists() {
        DocumentEntity document = new DocumentEntity();
        document.setId("doc-1");
        document.setAppointmentId("apt-1");
        document.setDocumentType("CONSULTATION_SUMMARY");
        document.setStorageKey("apt-1/doc-1.pdf");
        document.setBucket("clinical-documents");
        document.setPhysicianRef("phy-1");
        document.setIssuedAt(Instant.parse("2026-06-01T12:00:00Z"));
        when(documentRepository.findByAppointmentIdOrderByIssuedAtDesc("apt-1"))
                .thenReturn(java.util.List.of(document));
        when(prescriptionRepository.findByAppointmentIdOrderByIssuedAtDesc("apt-1"))
                .thenReturn(java.util.List.of());
        when(medicalLeaveRepository.findByAppointmentIdOrderByIssuedAtDesc("apt-1"))
                .thenReturn(java.util.List.of());

        var response = clinicalDocsService.listForAppointment("apt-1");

        assertThat(response.downloadUrl()).startsWith("http://clinical-docs/api/v1/clinical-docs/download/");
        verify(downloadTokenRepository).save(any());
    }
}
