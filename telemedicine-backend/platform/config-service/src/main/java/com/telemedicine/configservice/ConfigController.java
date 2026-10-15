package com.telemedicine.configservice;

import com.telemedicine.common.audit.Audited;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.annotation.IsAdmin;
import com.telemedicine.common.web.PageResponse;
import com.telemedicine.common.web.page.PageQuery;
import com.telemedicine.common.web.page.PageRequestSupport;
import com.telemedicine.configservice.dto.ConfigEntryDto;
import com.telemedicine.configservice.dto.ConfigRevisionDto;
import com.telemedicine.configservice.dto.ConfigSnapshotDto;
import com.telemedicine.configservice.dto.UpdateConfigRequest;
import com.telemedicine.configservice.service.ConfigService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private static final Set<String> HISTORY_SORT_FIELDS = Set.of("changedAt");

    private final ConfigService configService;
    private final PageRequestSupport pageRequestSupport;
    private final SecurityContextAccessor securityContext;

    public ConfigController(
            ConfigService configService,
            PageRequestSupport pageRequestSupport,
            SecurityContextAccessor securityContext) {
        this.configService = configService;
        this.pageRequestSupport = pageRequestSupport;
        this.securityContext = securityContext;
    }

    @GetMapping
    public ApiResponse<ConfigSnapshotDto> getSnapshot() {
        return ApiResponse.ok(configService.getSnapshot());
    }

    @GetMapping("/{key:.+}")
    public ApiResponse<ConfigEntryDto> getEntry(@PathVariable String key) {
        return ApiResponse.ok(configService.getEntry(key));
    }

    @PutMapping("/{key:.+}")
    @IsAdmin
    @Audited(action = "CONFIG_UPDATED", resourceType = "PlatformConfig")
    public ApiResponse<ConfigEntryDto> updateEntry(
            @PathVariable String key, @Valid @RequestBody UpdateConfigRequest request) {
        String changedBy = securityContext.currentUserId().orElse("system");
        return ApiResponse.ok(configService.updateEntry(key, request.value(), changedBy));
    }

    @GetMapping("/{key:.+}/history")
    public ApiResponse<PageResponse<ConfigRevisionDto>> getHistory(
            @PathVariable String key,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageQuery pageQuery =
                pageRequestSupport.resolve(page, size, sort, HISTORY_SORT_FIELDS);
        return ApiResponse.ok(configService.getHistory(key, pageQuery));
    }
}
