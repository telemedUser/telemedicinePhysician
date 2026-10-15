package com.telemedicine.reportingservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsService;
import com.telemedicine.reportingservice.service.KpiAggregationService;
import com.telemedicine.reportingservice.web.dto.JobRunResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/jobs")
public class InternalJobsController {

    private static final Logger log = LoggerFactory.getLogger(InternalJobsController.class);

    private final KpiAggregationService aggregationService;

    public InternalJobsController(KpiAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostMapping("/report-generation/run")
    @IsService
    public ApiResponse<JobRunResponse> runReportGeneration() {
        try {
            int processed =
                    aggregationService.generateDailySnapshots(LocalDate.now(ZoneOffset.UTC));
            return ApiResponse.ok(new JobRunResponse("report-generation", processed, "OK"));
        } catch (RuntimeException exception) {
            log.warn("Report generation job failed open: {}", exception.getMessage());
            return ApiResponse.ok(new JobRunResponse("report-generation", 0, "FAILED_OPEN"));
        }
    }
}
