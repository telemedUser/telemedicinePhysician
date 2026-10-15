package com.telemedicine.configservice;

import com.telemedicine.common.audit.Audited;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsAdmin;
import com.telemedicine.configservice.dto.FeatureFlagDto;
import com.telemedicine.configservice.dto.UpdateFeatureFlagRequest;
import com.telemedicine.configservice.service.FeatureFlagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config/flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping("/{flagKey}")
    public ApiResponse<FeatureFlagDto> getFlag(@PathVariable String flagKey) {
        return ApiResponse.ok(featureFlagService.getFlag(flagKey));
    }

    @PutMapping("/{flagKey}")
    @IsAdmin
    @Audited(action = "FEATURE_FLAG_UPDATED", resourceType = "FeatureFlag")
    public ApiResponse<FeatureFlagDto> upsertFlag(
            @PathVariable String flagKey, @RequestBody UpdateFeatureFlagRequest request) {
        return ApiResponse.ok(featureFlagService.upsertFlag(flagKey, request));
    }
}
