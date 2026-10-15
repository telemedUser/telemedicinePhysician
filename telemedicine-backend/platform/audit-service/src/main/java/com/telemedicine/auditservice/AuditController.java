package com.telemedicine.auditservice;

import com.telemedicine.auditservice.service.AuditService;
import com.telemedicine.auditservice.web.dto.AuditEventResponse;
import com.telemedicine.auditservice.web.dto.AuditExportRequest;
import com.telemedicine.auditservice.web.dto.AuditExportResponse;
import com.telemedicine.auditservice.web.dto.AuditIngestRequest;
import com.telemedicine.auditservice.web.dto.ChainVerifyResponse;
import com.telemedicine.auditservice.web.dto.ConsentLedgerResponse;
import com.telemedicine.auditservice.web.dto.RetentionRunResponse;
import com.telemedicine.common.audit.Audited;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsAdmin;
import com.telemedicine.common.security.annotation.IsService;
import com.telemedicine.common.security.annotation.RequiresRole;
import com.telemedicine.common.web.PageResponse;
import com.telemedicine.common.web.page.PageQuery;
import com.telemedicine.common.web.page.PageRequestSupport;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private static final Set<String> AUDIT_SORT_FIELDS = Set.of("occurredAt", "sequence");

    private final AuditService auditService;
    private final PageRequestSupport pageRequestSupport;

    public AuditController(AuditService auditService, PageRequestSupport pageRequestSupport) {
        this.auditService = auditService;
        this.pageRequestSupport = pageRequestSupport;
    }

    @PostMapping
    @RequiresRole({"SERVICE", "ADMIN"})
    @Audited(action = "AUDIT_INGESTED", resourceType = "AUDIT_EVENT")
    public ApiResponse<AuditEventResponse> ingest(@Valid @RequestBody AuditIngestRequest request) {
        return ApiResponse.ok(auditService.ingestHttp(request));
    }

    @GetMapping
    @IsAdmin
    public ApiResponse<PageResponse<AuditEventResponse>> query(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        PageQuery pageQuery = pageRequestSupport.resolve(page, size, sort, AUDIT_SORT_FIELDS);
        return ApiResponse.ok(auditService.query(actorId, action, resourceType, from, to, pageQuery));
    }

    @GetMapping("/consent")
    @IsAdmin
    public ApiResponse<List<ConsentLedgerResponse>> consent(@RequestParam String appointmentId) {
        return ApiResponse.ok(auditService.queryConsent(appointmentId));
    }

    @GetMapping("/chain/verify")
    @IsAdmin
    public ApiResponse<ChainVerifyResponse> verifyChain() {
        return ApiResponse.ok(auditService.verifyChain());
    }

    @PostMapping("/exports")
    @IsAdmin
    @Audited(action = "AUDIT_EXPORT_REQUESTED", resourceType = "AUDIT_EXPORT")
    public ApiResponse<AuditExportResponse> export(@RequestBody AuditExportRequest request) {
        return ApiResponse.ok(auditService.export(request));
    }

    @PostMapping("/retention/run")
    @IsService
    public ApiResponse<RetentionRunResponse> runRetention() {
        return ApiResponse.ok(auditService.runRetention());
    }
}
