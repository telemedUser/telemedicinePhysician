package com.telemedicine.physicianservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.physicianservice.api.dto.CloseConsultationRequest;
import com.telemedicine.physicianservice.api.dto.CloseConsultationResponse;
import com.telemedicine.physicianservice.service.ConsultationClosureService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Physician closure of a consultation (PRD §14.2). Closure is accepted and {@code
 * ConsultationClosed} emitted only when the minimum completion set is satisfied or an audited
 * exception is supplied (TM-DOC-009).
 */
@RestController
@RequestMapping("/api/v1/physicians/me/consultations")
public class ConsultationClosureController {

    private final ConsultationClosureService consultationClosureService;

    public ConsultationClosureController(ConsultationClosureService consultationClosureService) {
        this.consultationClosureService = consultationClosureService;
    }

    @PostMapping("/{appointmentId}/close")
    @IsPhysician
    public ApiResponse<CloseConsultationResponse> close(
            @PathVariable String appointmentId,
            @RequestHeader(value = "X-Market", defaultValue = "IN") String market,
            @Valid @RequestBody CloseConsultationRequest request) {
        return ApiResponse.ok(consultationClosureService.close(appointmentId, market, request));
    }
}
