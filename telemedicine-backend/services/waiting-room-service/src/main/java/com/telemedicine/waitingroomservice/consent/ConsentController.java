package com.telemedicine.waitingroomservice.consent;

import com.telemedicine.common.audit.Audited;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPatient;
import com.telemedicine.waitingroomservice.web.dto.ConsentGrantRequest;
import com.telemedicine.waitingroomservice.web.dto.ConsentGrantResponse;
import com.telemedicine.waitingroomservice.web.dto.ConsentStatusResponse;
import com.telemedicine.waitingroomservice.web.dto.ConsentWithdrawRequest;
import com.telemedicine.waitingroomservice.web.dto.ConsentWithdrawResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waiting-room/consent")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping("/platform")
    @IsPatient
    @Audited(action = "CONSENT_PLATFORM_GRANTED", resourceType = "Consent")
    public ApiResponse<ConsentGrantResponse> platformConsent(
            @Valid @RequestBody ConsentGrantRequest body) {
        return ApiResponse.ok(consentService.grantPlatform(body));
    }

    @PostMapping("/clinical")
    @IsPatient
    @Audited(action = "CONSENT_CLINICAL_GRANTED", resourceType = "Consent")
    public ApiResponse<ConsentGrantResponse> clinicalConsent(
            @Valid @RequestBody ConsentGrantRequest body) {
        return ApiResponse.ok(consentService.grantClinical(body));
    }

    @PostMapping("/withdraw")
    @IsPatient
    @Audited(action = "CONSENT_WITHDRAWN", resourceType = "Consent")
    public ApiResponse<ConsentWithdrawResponse> withdraw(
            @Valid @RequestBody ConsentWithdrawRequest body) {
        return ApiResponse.ok(consentService.withdraw(body));
    }

    @GetMapping("/{subjectId}")
    public ApiResponse<java.util.Map<String, Object>> consentStatus(
            @PathVariable String subjectId,
            @RequestParam String type,
            @RequestParam(required = false) String version) {
        ConsentStatusResponse status = consentService.status(subjectId, type, version);
        return ApiResponse.ok(status.toMap());
    }
}
