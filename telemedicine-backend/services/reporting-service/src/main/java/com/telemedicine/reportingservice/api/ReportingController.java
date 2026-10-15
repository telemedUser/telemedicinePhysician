package com.telemedicine.reportingservice.api;

import com.telemedicine.common.audit.Audited;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsAdmin;
import com.telemedicine.reportingservice.service.ReportExportService;
import com.telemedicine.reportingservice.service.ReportingService;
import com.telemedicine.reportingservice.web.dto.KpiFamilyResponse;
import com.telemedicine.reportingservice.web.dto.KpiResponse;
import com.telemedicine.reportingservice.web.dto.ReportExportRequest;
import com.telemedicine.reportingservice.web.dto.ReportExportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reporting")
public class ReportingController {

    private final ReportingService reportingService;
    private final ReportExportService reportExportService;

    public ReportingController(ReportingService reportingService, ReportExportService reportExportService) {
        this.reportingService = reportingService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/kpis")
    @IsAdmin
    public ApiResponse<KpiResponse> kpis() {
        return ApiResponse.ok(reportingService.allKpis());
    }

    @GetMapping("/kpis/{family}")
    @IsAdmin
    public ApiResponse<KpiFamilyResponse> familyKpis(@PathVariable String family) {
        return ApiResponse.ok(reportingService.familyKpis(family));
    }

    @PostMapping("/exports")
    @IsAdmin
    @Audited(action = "REPORT_EXPORT_REQUESTED", resourceType = "REPORTING_EXPORT")
    public ApiResponse<ReportExportResponse> export(@RequestBody ReportExportRequest request) {
        return ApiResponse.ok(reportExportService.export(request));
    }
}
