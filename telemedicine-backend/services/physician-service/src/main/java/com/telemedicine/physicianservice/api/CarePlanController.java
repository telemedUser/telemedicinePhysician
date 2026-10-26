package com.telemedicine.physicianservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.physicianservice.api.dto.CarePlanDto;
import com.telemedicine.physicianservice.service.CarePlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The D-08 care plan for one appointment: load (GET) and save (PUT). Scoped to the signed-in
 * physician; the whole plan is sent on each save.
 */
@RestController
@RequestMapping("/api/v1/physicians/me/consultations")
public class CarePlanController {

    private final CarePlanService carePlanService;
    private final SecurityContextAccessor securityContextAccessor;

    public CarePlanController(
            CarePlanService carePlanService, SecurityContextAccessor securityContextAccessor) {
        this.carePlanService = carePlanService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping("/{appointmentId}/care-plan")
    @IsPhysician
    public ApiResponse<CarePlanDto> get(@PathVariable String appointmentId) {
        return ApiResponse.ok(carePlanService.get(appointmentId));
    }

    @PutMapping("/{appointmentId}/care-plan")
    @IsPhysician
    public ApiResponse<CarePlanDto> save(
            @PathVariable String appointmentId,
            @RequestHeader(value = "X-Market", defaultValue = "IN") String market,
            @RequestBody CarePlanDto body) {
        String physicianId = securityContextAccessor.currentPhysicianId().orElse(null);
        return ApiResponse.ok(carePlanService.save(appointmentId, physicianId, market, body));
    }
}
