package com.telemedicine.clinicaldocsservice;

import com.telemedicine.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinical-docs")
public class ClinicalDocsController {
    @PostMapping
    public ApiResponse<Map<String, String>> issue(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(Map.of("documentId", UUID.randomUUID().toString(), "event", "ClinicalDocumentIssued"));
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<Map<String, String>> list(@PathVariable String appointmentId) {
        return ApiResponse.ok(Map.of("appointmentId", appointmentId, "downloadUrl", "/api/v1/clinical-docs/download/doc-1"));
    }
}
