package com.telemedicine.physicianbff;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.physicianbff.dto.AggregationStatus;
import com.telemedicine.physicianbff.dto.ConsultationResponse;
import com.telemedicine.physicianbff.dto.WorklistResponse;
import com.telemedicine.physicianbff.dto.downstream.ClinicalDocumentResponse;
import com.telemedicine.physicianbff.service.PhysicianConsultationService;
import com.telemedicine.physicianbff.service.PhysicianDocumentationService;
import com.telemedicine.physicianbff.service.PhysicianWorklistService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/bff/physician")
@IsPhysician
public class PhysicianWorklistController {

    private final PhysicianWorklistService worklistService;
    private final PhysicianConsultationService consultationService;
    private final PhysicianDocumentationService documentationService;

    public PhysicianWorklistController(
            PhysicianWorklistService worklistService,
            PhysicianConsultationService consultationService,
            PhysicianDocumentationService documentationService) {
        this.worklistService = worklistService;
        this.consultationService = consultationService;
        this.documentationService = documentationService;
    }

    @GetMapping("/worklist")
    public ApiResponse<WorklistResponse> worklist() {
        WorklistResponse response = worklistService.worklist();
        if (response.status() == AggregationStatus.DEGRADED) {
            return ApiResponse.ok(response, "Worklist aggregated with degraded downstream data");
        }
        return ApiResponse.ok(response);
    }

    @GetMapping("/consultation/{appointmentId}")
    public ApiResponse<ConsultationResponse> consultation(@PathVariable String appointmentId) {
        ConsultationResponse response = consultationService.consultation(appointmentId);
        if (response.status() == AggregationStatus.DEGRADED) {
            return ApiResponse.ok(response, "Consultation aggregated with degraded downstream data");
        }
        return ApiResponse.ok(response);
    }

    @PostMapping("/documentation/{appointmentId}")
    public ApiResponse<ClinicalDocumentResponse> documentation(
            @PathVariable String appointmentId, @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(documentationService.submitDocumentation(appointmentId, body));
    }
}
