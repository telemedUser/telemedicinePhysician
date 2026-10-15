package com.telemedicine.patientbff;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPatient;
import com.telemedicine.patientbff.dto.AggregationStatus;
import com.telemedicine.patientbff.dto.PatientJourneyResponse;
import com.telemedicine.patientbff.service.PatientJourneyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/patient")
@IsPatient
public class PatientJourneyController {

    private final PatientJourneyService patientJourneyService;

    public PatientJourneyController(PatientJourneyService patientJourneyService) {
        this.patientJourneyService = patientJourneyService;
    }

    @GetMapping("/journey/{appointmentId}")
    public ApiResponse<PatientJourneyResponse> journey(@PathVariable String appointmentId) {
        PatientJourneyResponse response = patientJourneyService.journey(appointmentId);
        if (response.status() == AggregationStatus.DEGRADED) {
            return ApiResponse.ok(response, "One or more journey fragments are degraded");
        }
        return ApiResponse.ok(response);
    }
}
