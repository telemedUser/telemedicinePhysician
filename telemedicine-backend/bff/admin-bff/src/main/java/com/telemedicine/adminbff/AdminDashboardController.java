package com.telemedicine.adminbff;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.annotation.IsAdmin;
import com.telemedicine.adminbff.dto.AdminConfigView;
import com.telemedicine.adminbff.dto.AuditQueryResponse;
import com.telemedicine.adminbff.dto.OperationalSnapshot;
import com.telemedicine.adminbff.dto.RealmRolesResponse;
import com.telemedicine.adminbff.dto.SchedulerJobsResponse;
import com.telemedicine.adminbff.service.AdminAggregationService;
import com.telemedicine.adminbff.service.AdminConfigService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/admin")
@IsAdmin
public class AdminDashboardController {

    private final AdminConfigService adminConfigService;
    private final AdminAggregationService adminAggregationService;

    public AdminDashboardController(
            AdminConfigService adminConfigService, AdminAggregationService adminAggregationService) {
        this.adminConfigService = adminConfigService;
        this.adminAggregationService = adminAggregationService;
    }

    @GetMapping("/config")
    public ApiResponse<AdminConfigView> config() {
        return ApiResponse.ok(adminConfigService.currentConfig());
    }

    @PutMapping("/config/{key}")
    public ApiResponse<Map<String, Object>> updateConfig(
            @PathVariable String key, @RequestBody Object value) {
        return ApiResponse.ok(adminAggregationService.updateConfig(key, value));
    }

    @GetMapping("/audit")
    public ApiResponse<AuditQueryResponse> audit() {
        return ApiResponse.ok(adminAggregationService.recentAudit());
    }

    @GetMapping("/scheduler")
    public ApiResponse<SchedulerJobsResponse> scheduler() {
        return ApiResponse.ok(adminAggregationService.schedulerJobs());
    }

    @GetMapping("/snapshot")
    public ApiResponse<OperationalSnapshot> snapshot() {
        return ApiResponse.ok(adminAggregationService.snapshot());
    }

    @GetMapping("/access/roles")
    public ApiResponse<RealmRolesResponse> realmRoles() {
        return ApiResponse.ok(new RealmRolesResponse(Roles.REALM_ROLES));
    }
}
