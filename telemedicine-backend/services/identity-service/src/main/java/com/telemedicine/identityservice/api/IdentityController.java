package com.telemedicine.identityservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPatient;
import com.telemedicine.common.security.consent.RequiresConsent;
import com.telemedicine.identityservice.api.dto.FonasaChallengeRequest;
import com.telemedicine.identityservice.api.dto.FonasaChallengeResponse;
import com.telemedicine.identityservice.api.dto.FonasaVerifyRequest;
import com.telemedicine.identityservice.api.dto.FonasaVerifyResponse;
import com.telemedicine.identityservice.api.dto.ValidateIdentityRequest;
import com.telemedicine.identityservice.api.dto.ValidateIdentityResponse;
import com.telemedicine.identityservice.service.IdentityService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/{appointmentId}/status")
    public ApiResponse<Map<String, Object>> status(@PathVariable String appointmentId) {
        return ApiResponse.ok(identityService.status(appointmentId));
    }

    @PostMapping("/validate")
    @IsPatient
    public ApiResponse<ValidateIdentityResponse> validate(
            @Valid @RequestBody ValidateIdentityRequest request) {
        return ApiResponse.ok(identityService.validate(request));
    }

    @PostMapping("/fonasa/2fa/challenge")
    @IsPatient
    @RequiresConsent(value = "PLATFORM_TERMS", subject = "#request.appointmentId()")
    public ApiResponse<FonasaChallengeResponse> challenge(
            @Valid @RequestBody FonasaChallengeRequest request) {
        return ApiResponse.ok(identityService.startFonasaChallenge(request));
    }

    @PostMapping("/fonasa/2fa/verify")
    @IsPatient
    @RequiresConsent(value = "PLATFORM_TERMS", subject = "#request.appointmentId()")
    public ApiResponse<FonasaVerifyResponse> verify(@Valid @RequestBody FonasaVerifyRequest request) {
        return ApiResponse.ok(identityService.verifyFonasa(request));
    }
}
