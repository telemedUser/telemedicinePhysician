package com.telemedicine.integrationservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.integration.ExternalSystems;
import com.telemedicine.common.integration.webhook.WebhookSignatureVerifier;
import com.telemedicine.integrationservice.service.WebhookService;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integration/webhooks")
@Profile("postgres")
public class WebhookController {

    private static final String TIMESTAMP_HEADER = "X-Webhook-Timestamp";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private final WebhookSignatureVerifier webhookSignatureVerifier;
    private final WebhookService webhookService;

    public WebhookController(
            WebhookSignatureVerifier webhookSignatureVerifier, WebhookService webhookService) {
        this.webhookSignatureVerifier = webhookSignatureVerifier;
        this.webhookService = webhookService;
    }

    @PostMapping("/{system}")
    public ApiResponse<Map<String, String>> receive(
            @PathVariable String system,
            @RequestBody String payload,
            @RequestHeader(TIMESTAMP_HEADER) long timestamp,
            @RequestHeader(SIGNATURE_HEADER) String signature) {
        webhookSignatureVerifier.verify(payload, timestamp, signature);
        if (ExternalSystems.SAP.equalsIgnoreCase(system)) {
            return ApiResponse.ok(webhookService.handleSapLinkRequest(parsePayload(payload)));
        }
        throw new DomainException(ErrorCode.NOT_FOUND, "Unsupported webhook system: " + system);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> parsePayload(String payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, Map.class);
        } catch (Exception exception) {
            throw new DomainException(ErrorCode.MALFORMED_REQUEST, "Invalid webhook payload");
        }
    }
}
