package com.telemedicine.surveyservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsService;
import com.telemedicine.surveyservice.api.dto.SubmitSurveyResponseRequest;
import com.telemedicine.surveyservice.api.dto.SurveyAvailabilityResponse;
import com.telemedicine.surveyservice.api.dto.SurveyDispatchJobResponse;
import com.telemedicine.surveyservice.config.SurveyServiceProperties;
import com.telemedicine.surveyservice.service.SurveyService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/survey")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @PostMapping("/{appointmentId}/responses")
    public ApiResponse<SurveyAvailabilityResponse> submitResponse(
            @PathVariable String appointmentId, @Valid @RequestBody SubmitSurveyResponseRequest request) {
        return ApiResponse.ok(surveyService.submitResponse(appointmentId, request));
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<SurveyAvailabilityResponse> availability(@PathVariable String appointmentId) {
        return ApiResponse.ok(surveyService.availability(appointmentId));
    }
}

@RestController
@RequestMapping("/api/v1/internal/jobs")
class SurveyInternalJobController {

    private static final Logger log = LoggerFactory.getLogger(SurveyInternalJobController.class);

    private final SurveyService surveyService;
    private final SurveyServiceProperties properties;

    SurveyInternalJobController(SurveyService surveyService, SurveyServiceProperties properties) {
        this.surveyService = surveyService;
        this.properties = properties;
    }

    @PostMapping("/survey-dispatch/run")
    @IsService
    public ApiResponse<SurveyDispatchJobResponse> runSurveyDispatch() {
        try {
            Instant cutoff = Instant.now().minus(properties.getDispatchBackupMinutes());
            int processed = surveyService.redispatchPendingSurveys(cutoff);
            return ApiResponse.ok(new SurveyDispatchJobResponse("survey-dispatch", processed, "OK"));
        } catch (RuntimeException exception) {
            log.warn("Survey dispatch job failed open: {}", exception.getMessage());
            return ApiResponse.ok(new SurveyDispatchJobResponse("survey-dispatch", 0, "FAILED_OPEN"));
        }
    }
}
