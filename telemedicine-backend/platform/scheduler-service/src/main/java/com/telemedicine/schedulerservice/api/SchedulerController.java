package com.telemedicine.schedulerservice.api;

import com.telemedicine.common.audit.Audited;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsAdmin;
import com.telemedicine.common.web.PageResponse;
import com.telemedicine.schedulerservice.api.dto.JobDefinitionDto;
import com.telemedicine.schedulerservice.api.dto.JobRunDto;
import com.telemedicine.schedulerservice.api.dto.UpdateJobRequest;
import com.telemedicine.schedulerservice.service.SchedulerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/jobs")
    public ApiResponse<List<JobDefinitionDto>> jobs() {
        return ApiResponse.ok(schedulerService.listJobs());
    }

    @PatchMapping("/jobs/{key}")
    @IsAdmin
    @Audited(action = "SCHEDULER_JOB_UPDATED", resourceType = "SchedulerJob")
    public ApiResponse<JobDefinitionDto> updateJob(
            @PathVariable String key, @RequestBody UpdateJobRequest request) {
        return ApiResponse.ok(schedulerService.updateJob(key, request));
    }

    @GetMapping("/runs")
    public ApiResponse<PageResponse<JobRunDto>> runs(
            @RequestParam(required = false) String jobKey,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(schedulerService.listRuns(jobKey, page, size, sort));
    }

    @PostMapping("/jobs/{key}/trigger")
    @IsAdmin
    @Audited(action = "SCHEDULER_JOB_TRIGGERED", resourceType = "SchedulerJob")
    public ApiResponse<Void> trigger(@PathVariable String key) {
        schedulerService.triggerJob(key);
        return ApiResponse.ok();
    }
}
