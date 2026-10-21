package com.telemedicine.physicianservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.physicianservice.api.dto.PatientDetailResponse;
import com.telemedicine.physicianservice.api.dto.WorklistEntryResponse;
import com.telemedicine.physicianservice.service.PatientDetailService;
import com.telemedicine.physicianservice.service.WorklistService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Physician worklist surface (PRD §12.3). The physician is resolved from the authenticated JWT
 * (HIS SSO assertion per TM-WLT-004); the market is carried on the request by the BFF.
 */
@RestController
@RequestMapping("/api/v1/physicians/me/worklist")
public class WorklistController {

    private final WorklistService worklistService;
    private final PatientDetailService patientDetailService;
    private final SecurityContextAccessor securityContextAccessor;

    public WorklistController(
            WorklistService worklistService,
            PatientDetailService patientDetailService,
            SecurityContextAccessor securityContextAccessor) {
        this.worklistService = worklistService;
        this.patientDetailService = patientDetailService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @org.springframework.web.bind.annotation.GetMapping
    @IsPhysician
    public ApiResponse<List<WorklistEntryResponse>> worklist(
            @RequestHeader(value = "X-Market", defaultValue = "IN") String market) {
        String physicianId = securityContextAccessor.currentPhysicianId().orElseThrow();
        return ApiResponse.ok(worklistService.worklistFor(physicianId, market));
    }

    /** Full review &amp; join detail for one appointment on the signed-in physician's worklist (D-03). */
    @GetMapping("/{appointmentId}")
    @IsPhysician
    public ApiResponse<PatientDetailResponse> detail(@PathVariable String appointmentId) {
        String physicianId = securityContextAccessor.currentPhysicianId().orElseThrow();
        return ApiResponse.ok(patientDetailService.detailFor(physicianId, appointmentId));
    }

    @PostMapping("/{appointmentId}/red-flag/ack")
    @IsPhysician
    public ApiResponse<Void> acknowledgeRedFlag(@PathVariable String appointmentId) {
        String physicianRef = securityContextAccessor.currentUser().actorId();
        worklistService.acknowledgeRedFlag(appointmentId, physicianRef);
        return ApiResponse.ok();
    }

    @PostMapping("/{appointmentId}/admit")
    @IsPhysician
    public ApiResponse<WorklistEntryResponse> admit(@PathVariable String appointmentId) {
        return ApiResponse.ok(worklistService.admit(appointmentId));
    }
}
