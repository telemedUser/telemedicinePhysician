package com.telemedicine.clinicaldocsservice.api;

import com.telemedicine.clinicaldocsservice.api.dto.AppointmentDocumentsResponse;
import com.telemedicine.clinicaldocsservice.api.dto.IssueDocumentRequest;
import com.telemedicine.clinicaldocsservice.api.dto.IssueMedicalLeaveRequest;
import com.telemedicine.clinicaldocsservice.api.dto.IssuePrescriptionRequest;
import com.telemedicine.clinicaldocsservice.service.ClinicalDocsService;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPhysician;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinical-docs")
public class ClinicalDocsController {

    private final ClinicalDocsService clinicalDocsService;

    public ClinicalDocsController(ClinicalDocsService clinicalDocsService) {
        this.clinicalDocsService = clinicalDocsService;
    }

    @PostMapping
    @IsPhysician
    public ApiResponse<Map<String, String>> issueDocument(@Valid @RequestBody IssueDocumentRequest request) {
        return ApiResponse.ok(clinicalDocsService.issueDocument(request));
    }

    @PostMapping("/{appointmentId}/prescriptions")
    @IsPhysician
    public ApiResponse<Map<String, String>> issuePrescription(
            @PathVariable String appointmentId, @Valid @RequestBody IssuePrescriptionRequest request) {
        return ApiResponse.ok(clinicalDocsService.issuePrescription(appointmentId, request));
    }

    @PostMapping("/{appointmentId}/medical-leave")
    @IsPhysician
    public ApiResponse<Map<String, String>> issueMedicalLeave(
            @PathVariable String appointmentId, @Valid @RequestBody IssueMedicalLeaveRequest request) {
        return ApiResponse.ok(clinicalDocsService.issueMedicalLeave(appointmentId, request));
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<AppointmentDocumentsResponse> listForAppointment(@PathVariable String appointmentId) {
        return ApiResponse.ok(clinicalDocsService.listForAppointment(appointmentId));
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<byte[]> download(@PathVariable String token) {
        byte[] body = clinicalDocsService.download(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clinical-document.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
