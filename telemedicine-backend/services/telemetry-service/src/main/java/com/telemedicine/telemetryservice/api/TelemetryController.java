package com.telemedicine.telemetryservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.consent.RequiresConsent;
import com.telemedicine.telemetryservice.service.TelemetryService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/token")
    @RequiresConsent(value = "CLINICAL_INFORMED", subject = "#body['appointmentId']")
    public ApiResponse<Map<String, String>> token(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                telemetryService.requestToken(
                        body.get("appointmentId"),
                        body.get("sessionId"),
                        body.get("appointmentType"),
                        body.get("specialty")));
    }

    @PostMapping("/results")
    @RequiresConsent(value = "CLINICAL_INFORMED", subject = "#body['appointmentId']")
    public ApiResponse<Map<String, Object>> results(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                telemetryService.submitResults(
                        body.get("appointmentId"),
                        body.get("sessionId"),
                        body.get("token"),
                        body.get("appointmentType"),
                        body.get("specialty")));
    }
}
