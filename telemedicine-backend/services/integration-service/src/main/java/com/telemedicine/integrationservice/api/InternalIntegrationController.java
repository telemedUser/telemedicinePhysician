package com.telemedicine.integrationservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsService;
import com.telemedicine.integrationservice.adapter.FonasaAdapter;
import com.telemedicine.integrationservice.adapter.HisAdapter;
import com.telemedicine.integrationservice.adapter.ZendeskAdapter;
import com.telemedicine.integrationservice.service.InternalIntegrationService;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integration")
@Profile("postgres")
public class InternalIntegrationController {

    private final InternalIntegrationService internalIntegrationService;

    public InternalIntegrationController(InternalIntegrationService internalIntegrationService) {
        this.internalIntegrationService = internalIntegrationService;
    }

    @PostMapping("/fonasa/2fa/challenge")
    @IsService
    public ApiResponse<FonasaAdapter.ChallengeResult> fonasaChallenge(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(internalIntegrationService.fonasaChallenge(body.get("patientId")));
    }

    @PostMapping("/fonasa/2fa/verify")
    @IsService
    public ApiResponse<FonasaAdapter.VerifyResult> fonasaVerify(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                internalIntegrationService.fonasaVerify(
                        body.get("patientId"), body.get("challengeId"), body.get("code")));
    }

    @PostMapping("/his/appointments/{id}/status")
    @IsService
    public ApiResponse<HisAdapter.HisStatusUpdate> updateHisStatus(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(internalIntegrationService.updateHisStatus(id, body.get("status")));
    }

    @PostMapping("/zendesk/tickets")
    @IsService
    public ApiResponse<ZendeskAdapter.TicketResult> createTicket(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(internalIntegrationService.createTicket(body));
    }
}
