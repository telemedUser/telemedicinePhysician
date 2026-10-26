package com.telemedicine.physicianservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.physicianservice.api.dto.ReferenceDtos.DiagnosisOption;
import com.telemedicine.physicianservice.api.dto.ReferenceDtos.DiagnosticTestOption;
import com.telemedicine.physicianservice.api.dto.ReferenceDtos.MedicationOption;
import com.telemedicine.physicianservice.service.ReferenceDirectoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clinical reference directories that back the D-08 care-plan dropdowns (diagnosis ICD-10,
 * medication formulary, diagnostic tests). DB-backed; a blank {@code q} returns a default slice.
 */
@RestController
@RequestMapping("/api/v1/physicians/reference")
public class ReferenceDirectoryController {

    private final ReferenceDirectoryService referenceDirectoryService;

    public ReferenceDirectoryController(ReferenceDirectoryService referenceDirectoryService) {
        this.referenceDirectoryService = referenceDirectoryService;
    }

    @GetMapping("/diagnoses")
    @IsPhysician
    public ApiResponse<List<DiagnosisOption>> diagnoses(
            @RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(referenceDirectoryService.searchDiagnoses(q));
    }

    @GetMapping("/medications")
    @IsPhysician
    public ApiResponse<List<MedicationOption>> medications(
            @RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(referenceDirectoryService.searchMedications(q));
    }

    @GetMapping("/diagnostic-tests")
    @IsPhysician
    public ApiResponse<List<DiagnosticTestOption>> diagnosticTests(
            @RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(referenceDirectoryService.searchDiagnosticTests(q));
    }
}
