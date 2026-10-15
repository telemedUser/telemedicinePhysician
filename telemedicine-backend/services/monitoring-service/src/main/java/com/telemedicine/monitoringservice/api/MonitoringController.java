package com.telemedicine.monitoringservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.common.security.annotation.RequiresRole;
import com.telemedicine.monitoringservice.service.MonitoringSnapshotService;
import com.telemedicine.monitoringservice.service.QualityService;
import com.telemedicine.monitoringservice.web.dto.MonitoringSnapshot;
import com.telemedicine.monitoringservice.web.dto.VideoQualityRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final MonitoringSnapshotService snapshotService;
    private final QualityService qualityService;

    public MonitoringController(MonitoringSnapshotService snapshotService, QualityService qualityService) {
        this.snapshotService = snapshotService;
        this.qualityService = qualityService;
    }

    @GetMapping("/snapshot")
    @RequiresRole({"ADMIN", "PHYSICIAN"})
    public ApiResponse<MonitoringSnapshot> snapshot() {
        return ApiResponse.ok(snapshotService.snapshot());
    }

    @PostMapping("/quality")
    @IsPhysician
    public ApiResponse<Map<String, String>> quality(@RequestBody VideoQualityRequest request) {
        qualityService.recordQuality(request);
        return ApiResponse.ok(Map.of("sessionId", request.sessionId(), "recorded", "true"));
    }
}
