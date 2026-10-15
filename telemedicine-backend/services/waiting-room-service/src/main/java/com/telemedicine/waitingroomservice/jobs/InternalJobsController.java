package com.telemedicine.waitingroomservice.jobs;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsService;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.status.StatusService;
import com.telemedicine.waitingroomservice.web.dto.JobRunResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/jobs")
public class InternalJobsController {

    private static final Logger log = LoggerFactory.getLogger(InternalJobsController.class);

    private final StatusService statusService;
    private final WaitingRoomServiceProperties properties;

    public InternalJobsController(StatusService statusService, WaitingRoomServiceProperties properties) {
        this.statusService = statusService;
        this.properties = properties;
    }

    @PostMapping("/waiting-room-timeout/run")
    @IsService
    public ApiResponse<JobRunResponse> runWaitingRoomTimeout() {
        try {
            int processed = statusService.expireTimedOutPatients(Instant.now());
            return ApiResponse.ok(new JobRunResponse("waiting-room-timeout", processed, "OK"));
        } catch (RuntimeException exception) {
            log.warn("Waiting-room timeout job failed open: {}", exception.getMessage());
            return ApiResponse.ok(new JobRunResponse("waiting-room-timeout", 0, "FAILED_OPEN"));
        }
    }

    @PostMapping("/no-show-detection/run")
    @IsService
    public ApiResponse<JobRunResponse> runNoShowDetection() {
        try {
            int processed =
                    statusService.detectNoShows(Instant.now(), properties.getNoShowThreshold());
            return ApiResponse.ok(new JobRunResponse("no-show-detection", processed, "OK"));
        } catch (RuntimeException exception) {
            log.warn("No-show detection job failed open: {}", exception.getMessage());
            return ApiResponse.ok(new JobRunResponse("no-show-detection", 0, "FAILED_OPEN"));
        }
    }
}
